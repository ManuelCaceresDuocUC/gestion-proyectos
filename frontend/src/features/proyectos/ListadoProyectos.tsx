import { useEffect, useState } from "react";
import { proyectosApi } from "../../api/planificacion";
import type { EstadoProyecto, Proyecto } from "../../types/planificacion";
import FormularioProyecto from "./FormularioProyecto";
import { ESTADOS_EDITABLES, ETIQUETA_ESTADO, dinero, fecha, mensajeDe, porcentaje } from "./formato";
import { puedeEditar } from "./permisos";

interface Props {
  abrir: (id: number) => void;
}

/** Listado de proyectos con filtros por estado y responsable (A-10). */
export default function ListadoProyectos({ abrir }: Props) {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [estado, setEstado] = useState("");
  const [responsable, setResponsable] = useState("");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [creando, setCreando] = useState(false);

  useEffect(() => {
    let vigente = true;
    setCargando(true);
    proyectosApi
      .listar(estado, responsable)
      .then((lista) => { if (vigente) { setProyectos(lista); setError(""); } })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [estado, responsable]);

  const estados: EstadoProyecto[] = [...ESTADOS_EDITABLES, "TERMINADO"];

  return (
    <section>
      <div className="encabezado">
        <h2>Proyectos</h2>
        {puedeEditar() && !creando && (
          <button className="boton primario" onClick={() => setCreando(true)}>Nuevo proyecto</button>
        )}
      </div>

      {creando && (
        <FormularioProyecto
          alGuardar={(p) => { setCreando(false); abrir(p.id); }}
          alCancelar={() => setCreando(false)}
        />
      )}

      <div className="filtros">
        <label>Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {estados.map((s) => <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>)}
          </select>
        </label>
        <label>Responsable (id)
          <input type="number" min={1} value={responsable} onChange={(e) => setResponsable(e.target.value)} />
        </label>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {cargando ? <p>Cargando…</p> : proyectos.length === 0 ? (
        <p className="vacio">No hay proyectos con esos filtros.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th><th>Nombre</th><th>Estado</th><th>Presupuesto</th><th>Avance</th><th>Inicio</th><th>Fin plan</th>
            </tr>
          </thead>
          <tbody>
            {proyectos.map((p) => (
              <tr key={p.id} className="fila-click" onClick={() => abrir(p.id)}>
                <td>{p.codigo}</td>
                <td>{p.nombre}</td>
                <td><span className={`etiqueta estado-${p.estado}`}>{ETIQUETA_ESTADO[p.estado]}</span></td>
                <td className="num">{dinero(p.presupuesto, p.moneda)}</td>
                <td>
                  <div className="progreso" title={porcentaje(p.avance)}><div style={{ width: `${Math.min(100, p.avance)}%` }} /></div>
                </td>
                <td>{fecha(p.fechaInicio)}</td>
                <td>{fecha(p.fechaFinPlan)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
