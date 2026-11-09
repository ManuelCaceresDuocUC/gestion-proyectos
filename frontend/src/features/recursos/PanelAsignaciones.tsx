import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { asignacionesApi, recursosApi } from "../../api/recursos";
import type { Asignacion, AsignacionDatos, Recurso } from "../../types/recursos";
import Confirmar from "../proyectos/Confirmar";
import { dinero, mensajeDe } from "../proyectos/formato";
import { numero } from "./formato";

interface Props {
  tareaId: number;
  codigo: string;
  nombre: string;
  moneda: string;
  costoPlan: number;
  costoReal: number;
  editable: boolean;
  sobreasignada: boolean;
  /** Recarga la EDT y las alertas después de crear, editar o quitar una asignación. */
  alCambiar: () => void;
}

/** Asignaciones de una tarea hoja, con costo planificado y real (B-10). */
export default function PanelAsignaciones({
  tareaId, codigo, nombre, moneda, costoPlan, costoReal, editable, sobreasignada, alCambiar,
}: Props) {
  const [asignaciones, setAsignaciones] = useState<Asignacion[]>([]);
  const [recursos, setRecursos] = useState<Recurso[]>([]);
  const [editando, setEditando] = useState<Asignacion | "nueva" | null>(null);
  const [aEliminar, setAEliminar] = useState<Asignacion | null>(null);
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(true);

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const [lista, catalogo] = await Promise.all([
        asignacionesApi.listarPorTarea(tareaId),
        recursosApi.listar(false),
      ]);
      setAsignaciones(lista);
      setRecursos(catalogo);
      setError("");
    } catch (e) {
      setError(mensajeDe(e));
    } finally {
      setCargando(false);
    }
  }, [tareaId]);

  useEffect(() => { void cargar(); }, [cargar]);

  const nombreDe = (id: number) => recursos.find((r) => r.id === id)?.nombre ?? `Recurso ${id}`;

  async function quitar(asignacion: Asignacion) {
    setAEliminar(null);
    try {
      await asignacionesApi.eliminar(asignacion.id);
      setEditando(null);
      await cargar();
      alCambiar();
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  return (
    <div className="tarjeta">
      <div className="encabezado">
        <h4 style={{ margin: 0 }}>
          Recursos de {codigo} {nombre}
          {sobreasignada && <span className="marca-sobreasignacion" style={{ marginLeft: 8 }}>Sobreasignada</span>}
        </h4>
        {editable && editando === null && (
          <button className="boton primario" onClick={() => setEditando("nueva")}>Asignar recurso</button>
        )}
      </div>
      <dl className="datos">
        <div><dt>Costo planificado de la tarea</dt><dd>{dinero(costoPlan, moneda)}</dd></div>
        <div><dt>Costo real de la tarea</dt><dd>{dinero(costoReal, moneda)}</dd></div>
      </dl>
      {error && <p className="error" role="alert">{error}</p>}
      {editando && (
        <FormularioAsignacion
          key={editando === "nueva" ? "nueva" : editando.id}
          tareaId={tareaId}
          recursos={recursos}
          asignacion={editando === "nueva" ? undefined : editando}
          alGuardar={() => { setEditando(null); void cargar(); alCambiar(); }}
          alCancelar={() => setEditando(null)}
        />
      )}
      {cargando ? (
        <p className="vacio">Cargando asignaciones…</p>
      ) : asignaciones.length === 0 ? (
        <p className="vacio">Esta tarea no tiene recursos asignados.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Recurso</th>
              <th className="num">Cantidad</th>
              <th className="num">Tarifa</th>
              <th className="num">Costo plan</th>
              <th className="num">Cantidad real</th>
              <th className="num">Costo real</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {asignaciones.map((a) => (
              <tr key={a.id}>
                <td>{nombreDe(a.recursoId)}</td>
                <td className="num">{a.cantidad}</td>
                <td className="num">{dinero(a.tarifa, moneda)}</td>
                <td className="num">{dinero(a.costoPlan, moneda)}</td>
                <td className="num">{a.cantidadReal}</td>
                <td className="num">{dinero(a.costoReal, moneda)}</td>
                <td>
                  {editable && (
                    <div className="acciones" style={{ marginTop: 0, justifyContent: "flex-end" }}>
                      <button className="boton chico" onClick={() => setEditando(a)}>Editar</button>
                      <button className="boton chico peligro" onClick={() => setAEliminar(a)}>Quitar</button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <p className="vacio">El costo planificado es cantidad × tarifa. El costo real es cantidad real × tarifa y queda en cero mientras no haya ejecución.</p>
      {aEliminar && (
        <Confirmar titulo="Quitar asignación" textoConfirmar="Quitar" peligro
          alConfirmar={() => void quitar(aEliminar)} alCancelar={() => setAEliminar(null)}>
          <p>¿Quitar {nombreDe(aEliminar.recursoId)} de {codigo}? El proyecto se recalcula.</p>
        </Confirmar>
      )}
    </div>
  );
}

function FormularioAsignacion({ tareaId, recursos, asignacion, alGuardar, alCancelar }: {
  tareaId: number;
  recursos: Recurso[];
  asignacion?: Asignacion;
  alGuardar: () => void;
  alCancelar: () => void;
}) {
  const activos = recursos.filter((r) => r.activo || r.id === asignacion?.recursoId);
  const [recursoId, setRecursoId] = useState(String(asignacion?.recursoId ?? activos[0]?.id ?? ""));
  const [cantidad, setCantidad] = useState(asignacion ? String(asignacion.cantidad) : "");
  const [tarifa, setTarifa] = useState(asignacion ? String(asignacion.tarifa) : "");
  const [horasSemana, setHorasSemana] = useState(asignacion?.horasSemana == null ? "" : String(asignacion.horasSemana));
  const [semanas, setSemanas] = useState(asignacion?.semanas == null ? "" : String(asignacion.semanas));
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const recurso = recursos.find((r) => r.id === Number(recursoId));

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!recursoId) {
      setError("No hay recursos activos para asignar.");
      return;
    }
    setGuardando(true);
    setError("");
    const datos: AsignacionDatos = {
      tareaId,
      recursoId: Number(recursoId),
      cantidad: numero(cantidad),
      ...(tarifa.trim() ? { tarifa: numero(tarifa) } : {}),
      ...(horasSemana.trim() ? { horasSemana: numero(horasSemana) } : {}),
      ...(semanas.trim() ? { semanas: numero(semanas) } : {}),
    };
    try {
      if (asignacion) await asignacionesApi.actualizar(asignacion.id, datos);
      else await asignacionesApi.crear(datos);
      alGuardar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <div className="rejilla">
        <label>Recurso
          <select value={recursoId} onChange={(e) => setRecursoId(e.target.value)} required>
            {activos.length === 0 && <option value="">Sin recursos activos</option>}
            {activos.map((r) => (
              <option key={r.id} value={r.id}>{r.nombre}{r.activo ? "" : " (inactivo)"}</option>
            ))}
          </select>
        </label>
        <label>Cantidad (horas si es profesional)
          <input type="number" min={0.01} step="any" value={cantidad} onChange={(e) => setCantidad(e.target.value)} required />
        </label>
        <label>Tarifa{recurso ? ` (vacío usa ${dinero(recurso.tarifa, "CLP")})` : ""}
          <input type="number" min={0} step="any" value={tarifa} onChange={(e) => setTarifa(e.target.value)} placeholder={recurso ? String(recurso.tarifa) : ""} />
        </label>
        <label>Horas por semana (opcional)
          <input type="number" min={0} step="any" value={horasSemana} onChange={(e) => setHorasSemana(e.target.value)} />
        </label>
        <label>Semanas (opcional)
          <input type="number" min={0} step="any" value={semanas} onChange={(e) => setSemanas(e.target.value)} />
        </label>
      </div>
      <p className="aviso">En un profesional la cantidad son las horas totales de la tarea. El recálculo reparte esa carga en los días hábiles.</p>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="acciones">
        <button type="submit" className="boton primario" disabled={guardando || activos.length === 0}>
          {guardando ? "Guardando…" : "Guardar asignación"}
        </button>
        <button type="button" className="boton" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
