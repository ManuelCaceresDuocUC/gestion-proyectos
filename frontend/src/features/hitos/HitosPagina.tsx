import { useEffect, useState } from "react";
import { hitosApi } from "../../api/hitos";
import { edtApi, proyectosApi } from "../../api/planificacion";
import type { Hito } from "../../types/hitos";
import type { NodoEdt, Proyecto } from "../../types/planificacion";
import { fecha, mensajeDe } from "../proyectos/formato";
import { puedeEditar } from "../proyectos/permisos";
import FormularioHito from "./FormularioHito";
import { ETIQUETA_ESTADO_HITO } from "./formato";

function aplanar(nodos: NodoEdt[], destino: NodoEdt[] = []): NodoEdt[] {
  for (const nodo of nodos) {
    if (nodo.tipo !== "ETAPA") destino.push(nodo);
    if (nodo.hijos.length > 0) aplanar(nodo.hijos, destino);
  }
  return destino;
}

/** Listado y edición de hitos del proyecto (B-11). */
export default function HitosPagina() {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [proyectoId, setProyectoId] = useState("");
  const [lista, setLista] = useState<Hito[]>([]);
  const [actividades, setActividades] = useState<NodoEdt[]>([]);
  const [formulario, setFormulario] = useState<Hito | "nuevo" | null>(null);
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const [tick, setTick] = useState(0);

  const proyecto = proyectos.find((p) => String(p.id) === proyectoId);
  const cerrado = proyecto?.estado === "TERMINADO";
  const editar = puedeEditar() && !cerrado;

  useEffect(() => {
    let vigente = true;
    proyectosApi.listar("", "")
      .then((p) => { if (vigente) setProyectos(p); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!proyectoId) {
      setLista([]);
      setActividades([]);
      return;
    }
    let vigente = true;
    setCargando(true);
    const id = Number(proyectoId);
    Promise.all([hitosApi.listar(id), edtApi.arbol(id)])
      .then(([hitos, arbol]) => {
        if (!vigente) return;
        setLista(hitos);
        setActividades(aplanar(arbol));
        setError("");
      })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [proyectoId, tick]);

  return (
    <section>
      <div className="encabezado">
        <h2>Hitos</h2>
        {editar && proyectoId && !formulario && (
          <button className="boton primario" onClick={() => setFormulario("nuevo")}>Nuevo hito</button>
        )}
      </div>
      <p className="vacio">Logro verificable de duración cero. No es una tarea de la carta Gantt.</p>

      <div className="filtros">
        <label>Proyecto
          <select value={proyectoId} onChange={(e) => { setProyectoId(e.target.value); setFormulario(null); }}>
            <option value="">Elija un proyecto</option>
            {proyectos.map((p) => (
              <option key={p.id} value={p.id}>{p.codigo} — {p.nombre}</option>
            ))}
          </select>
        </label>
      </div>

      {cerrado && <p className="aviso">El proyecto está cerrado y no admite cambios.</p>}
      {error && <p className="error" role="alert">{error}</p>}

      {formulario && proyectoId && (
        <FormularioHito
          key={formulario === "nuevo" ? "nuevo" : formulario.id}
          proyectoId={Number(proyectoId)}
          actividades={actividades}
          hito={formulario === "nuevo" ? undefined : formulario}
          alGuardar={() => { setFormulario(null); setTick((n) => n + 1); }}
          alCancelar={() => setFormulario(null)}
          alEliminar={formulario === "nuevo" ? undefined : () => { setFormulario(null); setTick((n) => n + 1); }}
        />
      )}

      {!proyectoId && <p className="vacio">Elija un proyecto para ver sus hitos.</p>}
      {proyectoId && cargando && <p className="vacio">Cargando hitos…</p>}
      {proyectoId && !cargando && lista.length === 0 && !formulario && (
        <p className="vacio">Este proyecto no tiene hitos.</p>
      )}

      {lista.length > 0 && (
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th>
              <th>Logro</th>
              <th>Semana</th>
              <th>Fecha prevista</th>
              <th>Actividades</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {lista.map((h) => (
              <tr key={h.id}>
                <td>{h.codigo}</td>
                <td>{h.logro}</td>
                <td className="num">{h.semanaPrevista}</td>
                <td>{fecha(h.fechaPlan)}</td>
                <td>{h.actividades.map((a) => a.codigo).join(", ")}</td>
                <td>{ETIQUETA_ESTADO_HITO[h.estado]}</td>
                <td>
                  {editar && (
                    <button className="boton chico" onClick={() => setFormulario(h)}>Editar</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
