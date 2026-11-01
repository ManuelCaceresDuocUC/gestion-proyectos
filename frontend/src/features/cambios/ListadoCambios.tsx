import { useEffect, useState } from "react";
import { cambiosApi } from "../../api/cambios";
import { proyectosApi } from "../../api/planificacion";
import type { FiltrosSolicitud, SolicitudCambio } from "../../types/cambios";
import type { Proyecto } from "../../types/planificacion";
import FormularioCambio from "./FormularioCambio";
import {
  ESTADOS_SOLICITUD,
  ETIQUETA_ESTADO,
  ETIQUETA_PRIORIDAD,
  ETIQUETA_TIPO,
  PRIORIDADES,
  TIPOS_SOLICITUD,
  fecha,
  mensajeDe,
  porcentaje,
} from "./formato";
import { puedeEditar } from "./permisos";

/** Listado de solicitudes de un proyecto, con filtros y formulario (C-06). */
export default function ListadoCambios() {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [proyectoId, setProyectoId] = useState("");
  const [lista, setLista] = useState<SolicitudCambio[]>([]);
  const [estado, setEstado] = useState("");
  const [tipo, setTipo] = useState("");
  const [prioridad, setPrioridad] = useState("");
  const [responsable, setResponsable] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");
  const [formulario, setFormulario] = useState<SolicitudCambio | "nueva" | null>(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let vigente = true;
    proyectosApi
      .listar("", "")
      .then((p) => { if (vigente) setProyectos(p); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!proyectoId) {
      setLista([]);
      return;
    }
    let vigente = true;
    setCargando(true);
    const filtros: FiltrosSolicitud = {
      estado: estado as FiltrosSolicitud["estado"],
      tipo: tipo as FiltrosSolicitud["tipo"],
      prioridad: prioridad as FiltrosSolicitud["prioridad"],
      responsableId: responsable,
      desde,
      hasta,
    };
    cambiosApi
      .listar(Number(proyectoId), filtros)
      .then((filas) => { if (vigente) { setLista(filas); setError(""); } })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [proyectoId, estado, tipo, prioridad, responsable, desde, hasta, tick]);

  return (
    <section>
      <div className="encabezado">
        <h2>Gestión del cambio</h2>
        {puedeEditar() && !formulario && proyectoId && (
          <button className="boton primario" onClick={() => setFormulario("nueva")}>Nueva solicitud</button>
        )}
      </div>

      {formulario && proyectoId && (
        <FormularioCambio
          key={formulario === "nueva" ? "nueva" : formulario.id}
          proyectoId={Number(proyectoId)}
          solicitud={formulario === "nueva" ? undefined : formulario}
          alGuardar={() => { setFormulario(null); setTick((n) => n + 1); }}
          alCancelar={() => setFormulario(null)}
        />
      )}

      <div className="filtros">
        <label>Proyecto
          <select value={proyectoId} onChange={(e) => { setProyectoId(e.target.value); setFormulario(null); }}>
            <option value="">Elija un proyecto</option>
            {proyectos.map((p) => (
              <option key={p.id} value={p.id}>{p.codigo} — {p.nombre}</option>
            ))}
          </select>
        </label>
        <label>Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {ESTADOS_SOLICITUD.map((s) => <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>)}
          </select>
        </label>
        <label>Tipo
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            <option value="">Todos</option>
            {TIPOS_SOLICITUD.map((s) => <option key={s} value={s}>{ETIQUETA_TIPO[s]}</option>)}
          </select>
        </label>
        <label>Prioridad
          <select value={prioridad} onChange={(e) => setPrioridad(e.target.value)}>
            <option value="">Todas</option>
            {PRIORIDADES.map((s) => <option key={s} value={s}>{ETIQUETA_PRIORIDAD[s]}</option>)}
          </select>
        </label>
        <label>Responsable (id)
          <input type="number" min={1} value={responsable} onChange={(e) => setResponsable(e.target.value)} />
        </label>
        <label>Desde
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </label>
        <label>Hasta
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </label>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      {!proyectoId ? (
        <p className="vacio">Elija un proyecto para ver sus solicitudes.</p>
      ) : cargando ? (
        <p>Cargando…</p>
      ) : lista.length === 0 ? (
        <p className="vacio">No hay solicitudes con esos filtros.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th>
              <th>Nombre</th>
              <th>Tipo</th>
              <th>Estado</th>
              <th>Prioridad</th>
              <th>Solicitud</th>
              <th>Fin previsto</th>
              <th>Atraso</th>
              <th>Avance</th>
            </tr>
          </thead>
          <tbody>
            {lista.map((s) => (
              <tr
                key={s.id}
                className={puedeEditar() ? "fila-click" : undefined}
                onClick={puedeEditar() ? () => setFormulario(s) : undefined}
              >
                <td>{s.codigo}</td>
                <td>{s.nombre}</td>
                <td>{ETIQUETA_TIPO[s.tipo]}</td>
                <td><span className={`etiqueta estado-${s.estado}`}>{ETIQUETA_ESTADO[s.estado]}</span></td>
                <td>{ETIQUETA_PRIORIDAD[s.prioridad]}</td>
                <td>{fecha(s.fechaSolicitud)}</td>
                <td>{fecha(s.finPrevisto)}</td>
                <td className="num">{s.diasAtraso}</td>
                <td>
                  <div className="progreso" title={porcentaje(s.avance)}>
                    <div style={{ width: `${Math.min(100, s.avance)}%` }} />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
