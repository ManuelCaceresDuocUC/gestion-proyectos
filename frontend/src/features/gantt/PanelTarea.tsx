import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ganttApi } from "../../api/gantt";
import { dependenciasApi } from "../../api/planificacion";
import type { TipoDependencia } from "../../types";
import type { CambioFecha, DependenciaDto } from "../../types/planificacion";
import { fecha, mensajeDe } from "../proyectos/formato";
import type { FilaGantt } from "./modelo";

const TIPOS: TipoDependencia[] = ["FS", "SS", "FF", "SF"];

interface Props {
  fila: FilaGantt;
  dependencias: DependenciaDto[];
  nombreDe: (id: number) => string;
  /** A-17: solo ADMIN edita. */
  editable: boolean;
  /** Notifica los cambios de fechas devueltos por el recálculo y recarga el diagrama. */
  alRecalcular: (cambios: CambioFecha[]) => void;
}

/** Detalle de la tarea seleccionada con edición de duración y dependencias (A-17). */
export default function PanelTarea({ fila, dependencias, nombreDe, editable, alRecalcular }: Props) {
  const { nodo, comp } = fila;
  const [duracion, setDuracion] = useState(String(nodo.duracionDias ?? 1));
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);

  useEffect(() => { setDuracion(String(nodo.duracionDias ?? 1)); setError(""); }, [nodo.id, nodo.duracionDias]);

  const propias = dependencias.filter((d) => d.predecesoraId === nodo.id || d.sucesoraId === nodo.id);

  async function ejecutar(accion: () => Promise<CambioFecha[]>) {
    setError("");
    setOcupado(true);
    try {
      alRecalcular(await accion());
    } catch (e) {
      setError(mensajeDe(e));
    } finally {
      setOcupado(false);
    }
  }

  function aplicarDuracion(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    void ejecutar(async () => (await ganttApi.cambiarDuracion(nodo.id, Number(duracion))).cambios);
  }

  return (
    <aside className="tarjeta g-panel">
      <h3>{nodo.codigo} {nodo.nombre}</h3>
      <dl className="datos">
        <div><dt>Plan</dt><dd>{fecha(nodo.inicioPlan)} → {fecha(nodo.finPlan)}</dd></div>
        <div><dt>Línea base</dt><dd>{comp?.inicioBase ? `${fecha(comp.inicioBase)} → ${fecha(comp.finBase)}` : "Sin base"}</dd></div>
        <div><dt>Real</dt><dd>{comp?.inicioReal ? `${fecha(comp.inicioReal)} → ${fecha(comp.finReal)}` : "—"}</dd></div>
        <div><dt>Desviación del fin</dt><dd>{comp?.desviacionFinDias == null ? "—" : `${comp.desviacionFinDias > 0 ? "+" : ""}${comp.desviacionFinDias} d hábiles`}</dd></div>
        <div><dt>Avance</dt><dd>{Math.round(nodo.avance)} %</dd></div>
        <div><dt>Estado</dt><dd>{comp?.atrasada ? "Atrasada" : "En plazo"}</dd></div>
      </dl>

      {editable && !fila.esResumen && (
        <form className="formulario" onSubmit={aplicarDuracion}>
          <label>Duración (días hábiles, 0 = hito)
            <input type="number" min={0} value={duracion} onChange={(e) => setDuracion(e.target.value)} required />
          </label>
          <button type="submit" className="boton primario" disabled={ocupado}>Aplicar y recalcular</button>
        </form>
      )}
      {fila.esResumen && <p className="aviso">Las fechas de este nodo se calculan desde sus subtareas.</p>}

      <h4>Dependencias</h4>
      {propias.length === 0 ? <p className="vacio">Sin dependencias.</p> : propias.map((d) => (
        <FilaDependencia key={`${d.id}-${d.tipo}-${d.desfaseDias}`} dep={d} nombreDe={nombreDe} editable={editable} ocupado={ocupado}
          guardar={(tipo, desfase) => ejecutar(async () => (await dependenciasApi.actualizar(d.id, tipo, desfase)).cambios)}
          quitar={() => ejecutar(async () => (await dependenciasApi.eliminar(d.id)).cambios)} />
      ))}
      {error && <p className="error" role="alert">{error}</p>}
    </aside>
  );
}

interface FilaDepProps {
  dep: DependenciaDto;
  nombreDe: (id: number) => string;
  editable: boolean;
  ocupado: boolean;
  guardar: (tipo: TipoDependencia, desfase: number) => Promise<void>;
  quitar: () => Promise<void>;
}

function FilaDependencia({ dep, nombreDe, editable, ocupado, guardar, quitar }: FilaDepProps) {
  const [tipo, setTipo] = useState<TipoDependencia>(dep.tipo);
  const [desfase, setDesfase] = useState(String(dep.desfaseDias));
  return (
    <div className="g-dep">
      <span>{nombreDe(dep.predecesoraId)} → {nombreDe(dep.sucesoraId)}</span>
      {editable ? (
        <span className="acciones">
          <select value={tipo} onChange={(e) => setTipo(e.target.value as TipoDependencia)}>
            {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <input type="number" value={desfase} onChange={(e) => setDesfase(e.target.value)} style={{ width: 70 }} title="Desfase (días hábiles)" />
          <button className="boton chico" disabled={ocupado} onClick={() => void guardar(tipo, Number(desfase))}>Guardar</button>
          <button className="boton chico peligro" disabled={ocupado} onClick={() => void quitar()}>Quitar</button>
        </span>
      ) : (
        <span className="meta">{dep.tipo} {dep.desfaseDias ? `${dep.desfaseDias} d` : ""}</span>
      )}
    </div>
  );
}
