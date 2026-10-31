import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { dependenciasApi } from "../../api/planificacion";
import type { CambioFecha, DependenciaDto, NodoEdt } from "../../types/planificacion";
import type { TipoDependencia } from "../../types";
import { fecha, mensajeDe } from "./formato";

const TIPOS: Array<[TipoDependencia, string]> = [
  ["FS", "FS · fin a inicio"], ["SS", "SS · inicio a inicio"], ["FF", "FF · fin a fin"], ["SF", "SF · inicio a fin"],
];

interface Props {
  proyectoId: number;
  arbol: NodoEdt[];
  editable: boolean;
  /** Se llama tras crear o eliminar una dependencia, porque las fechas pueden haber cambiado. */
  alCambiar: () => void;
}

function hojas(nodos: NodoEdt[]): NodoEdt[] {
  return nodos.flatMap((n) => (n.hijos.length === 0 ? [n] : hojas(n.hijos)));
}

/** Dependencias entre tareas hoja; muestra el error de ciclo del backend y las fechas que se movieron (A-11). */
export default function PanelDependencias({ proyectoId, arbol, editable, alCambiar }: Props) {
  const [lista, setLista] = useState<DependenciaDto[]>([]);
  const [pred, setPred] = useState("");
  const [suc, setSuc] = useState("");
  const [tipo, setTipo] = useState<TipoDependencia>("FS");
  const [desfase, setDesfase] = useState("0");
  const [error, setError] = useState("");
  const [cambios, setCambios] = useState<CambioFecha[]>([]);

  const tareas = hojas(arbol);
  const nombre = (id: number) => {
    const t = tareas.find((x) => x.id === id);
    return t ? `${t.codigo} ${t.nombre}` : `#${id}`;
  };

  const cargar = useCallback(() => {
    dependenciasApi.listar(proyectoId).then(setLista).catch((e) => setError(mensajeDe(e)));
  }, [proyectoId]);

  useEffect(() => { cargar(); }, [cargar, arbol]);

  async function agregar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setCambios([]);
    try {
      const r = await dependenciasApi.crear(proyectoId, Number(pred), Number(suc), tipo, Number(desfase));
      setCambios(r.cambios);
      setPred(""); setSuc(""); setDesfase("0");
      alCambiar();
    } catch (err) {
      setError(mensajeDe(err)); // p. ej. CICLO_EN_DEPENDENCIAS: el mensaje del backend indica el camino del ciclo
    }
  }

  async function quitar(id: number) {
    setError("");
    setCambios([]);
    try {
      const r = await dependenciasApi.eliminar(id);
      setCambios(r.cambios);
      alCambiar();
    } catch (err) {
      setError(mensajeDe(err));
    }
  }

  return (
    <div className="tarjeta">
      <h3>Dependencias</h3>
      {lista.length === 0 ? <p className="vacio">Sin dependencias.</p> : (
        <table className="tabla">
          <thead><tr><th>Predecesora</th><th>Tipo</th><th>Sucesora</th><th>Desfase (d)</th>{editable && <th />}</tr></thead>
          <tbody>
            {lista.map((d) => (
              <tr key={d.id}>
                <td>{nombre(d.predecesoraId)}</td><td>{d.tipo}</td><td>{nombre(d.sucesoraId)}</td><td>{d.desfaseDias}</td>
                {editable && <td><button className="boton chico peligro" onClick={() => void quitar(d.id)}>Quitar</button></td>}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editable && (
        <form className="formulario" onSubmit={agregar}>
          <div className="rejilla">
            <label>Predecesora
              <select value={pred} onChange={(e) => setPred(e.target.value)} required>
                <option value="">Seleccione…</option>
                {tareas.map((t) => <option key={t.id} value={t.id}>{t.codigo} {t.nombre}</option>)}
              </select>
            </label>
            <label>Tipo
              <select value={tipo} onChange={(e) => setTipo(e.target.value as TipoDependencia)}>
                {TIPOS.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
              </select>
            </label>
            <label>Sucesora
              <select value={suc} onChange={(e) => setSuc(e.target.value)} required>
                <option value="">Seleccione…</option>
                {tareas.filter((t) => String(t.id) !== pred).map((t) => <option key={t.id} value={t.id}>{t.codigo} {t.nombre}</option>)}
              </select>
            </label>
            <label>Desfase (días hábiles)
              <input type="number" value={desfase} onChange={(e) => setDesfase(e.target.value)} required />
            </label>
          </div>
          <button type="submit" className="boton primario">Agregar dependencia</button>
        </form>
      )}

      {error && <p className="error" role="alert">{error}</p>}
      {cambios.length > 0 && (
        <div className="exito">
          Fechas recalculadas:
          <ul>
            {cambios.map((c) => (
              <li key={c.tareaId}>{c.codigo} {c.nombre}: {fecha(c.inicioAntes)}→{fecha(c.finAntes)} pasó a {fecha(c.inicioDespues)}→{fecha(c.finDespues)}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
