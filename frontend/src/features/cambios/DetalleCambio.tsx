import { useCallback, useEffect, useState } from "react";
import { cambiosApi } from "../../api/cambios";
import type { CambioEtapa, SolicitudCambio } from "../../types/cambios";
import type { EstadoEtapa } from "../../types";
import FormularioCambio from "./FormularioCambio";
import {
  ESTADOS_ETAPA,
  ETIQUETA_ESTADO,
  ETIQUETA_ETAPA,
  ETIQUETA_IMPACTO,
  ETIQUETA_PRIORIDAD,
  ETIQUETA_RIESGO,
  ETIQUETA_TIPO,
  datosDe,
  dinero,
  fecha,
  mensajeDe,
  porcentaje,
} from "./formato";
import { puedeAprobar, puedeEditar } from "./permisos";
import {
  puedeAplicarAlPlan,
  puedeAprobarDesde,
  puedeCerrar,
  puedeEnviarAEvaluacion,
  puedeRechazarDesde,
} from "./transiciones";

interface Props {
  id: number;
  volver: () => void;
}

/** Ficha de una solicitud: impacto, etapas, aprobar/aplicar. Evidencias e historial son C-05. */
export default function DetalleCambio({ id, volver }: Props) {
  const [solicitud, setSolicitud] = useState<SolicitudCambio | null>(null);
  const [etapas, setEtapas] = useState<CambioEtapa[]>([]);
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [editando, setEditando] = useState(false);

  const cargar = useCallback(async () => {
    try {
      const [s, e] = await Promise.all([cambiosApi.obtener(id), cambiosApi.etapas(id)]);
      setSolicitud(s);
      setEtapas(e.etapas);
      setError("");
    } catch (err) {
      setError(mensajeDe(err));
    }
  }, [id]);

  useEffect(() => { void cargar(); }, [cargar]);

  async function mutar(accion: () => Promise<SolicitudCambio>) {
    setOcupado(true);
    try {
      setSolicitud(await accion());
      const e = await cambiosApi.etapas(id);
      setEtapas(e.etapas);
      setError("");
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setOcupado(false);
    }
  }

  async function cambiarEtapa(numero: number, estado: EstadoEtapa) {
    setOcupado(true);
    try {
      await cambiosApi.parchearEtapa(id, numero, estado);
      await cargar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setOcupado(false);
    }
  }

  if (!solicitud) {
    return (
      <section>
        {error ? <p className="error" role="alert">{error}</p> : <p>Cargando…</p>}
        <button className="boton" onClick={volver}>← Solicitudes</button>
      </section>
    );
  }

  const s = solicitud;
  const editable = puedeEditar();
  const faltaTareaTiempo = s.tipo === "TIEMPO" && s.tareaId == null;
  const aplicaSinGantt = s.tipo !== "TIEMPO" && puedeAplicarAlPlan(s.estado, s.aplicada);

  return (
    <section>
      <button className="boton" onClick={volver}>← Solicitudes</button>
      <div className="encabezado">
        <h2>
          {s.codigo} · {s.nombre}{" "}
          <span className={`etiqueta estado-${s.estado}`}>{ETIQUETA_ESTADO[s.estado]}</span>
        </h2>
        {editable && !editando && (
          <div className="acciones">
            <button className="boton" onClick={() => setEditando(true)} disabled={ocupado}>Editar</button>
          </div>
        )}
      </div>
      {error && <p className="error" role="alert">{error}</p>}

      {editando ? (
        <FormularioCambio
          proyectoId={s.proyectoId}
          solicitud={s}
          alGuardar={(guardada) => { setSolicitud(guardada); setEditando(false); void cargar(); }}
          alCancelar={() => setEditando(false)}
          alEliminar={volver}
        />
      ) : (
        <>
          <dl className="datos">
            <div><dt>Tipo</dt><dd>{ETIQUETA_TIPO[s.tipo]}</dd></div>
            <div><dt>Prioridad</dt><dd>{ETIQUETA_PRIORIDAD[s.prioridad]}</dd></div>
            <div><dt>Riesgo</dt><dd>{ETIQUETA_RIESGO[s.riesgo]}</dd></div>
            <div><dt>Impacto</dt><dd>{ETIQUETA_IMPACTO[s.impacto]}</dd></div>
            <div><dt>Solicitante</dt><dd>{s.solicitante ?? "—"}</dd></div>
            <div><dt>Responsable (id)</dt><dd>{s.responsableId ?? "—"}</dd></div>
            <div><dt>Solicitud</dt><dd>{fecha(s.fechaSolicitud)}</dd></div>
            <div><dt>Inicio previsto</dt><dd>{fecha(s.inicioPrevisto)}</dd></div>
            <div><dt>Fin previsto</dt><dd>{fecha(s.finPrevisto)}</dd></div>
            <div><dt>Fin real</dt><dd>{fecha(s.finReal)}</dd></div>
            <div><dt>Días de atraso</dt><dd>{s.diasAtraso}</dd></div>
            <div><dt>Tarea afectada</dt><dd>{s.tareaId ?? "—"}</dd></div>
            <div><dt>Impacto (días)</dt><dd>{s.impactoDias}</dd></div>
            <div><dt>Impacto costo</dt><dd>{dinero(s.impactoCosto)}</dd></div>
            <div><dt>Presupuesto previsto</dt><dd>{s.presupuestoPrevisto == null ? "—" : dinero(s.presupuestoPrevisto)}</dd></div>
            <div><dt>Costo real</dt><dd>{s.costoReal == null ? "—" : dinero(s.costoReal)}</dd></div>
            <div>
              <dt>Aplicada al plan</dt>
              <dd>{s.aplicada ? `Sí (${fecha(s.aplicadaEn)})` : "No"}</dd>
            </div>
            <div><dt>Aprobada</dt><dd>{s.aprobadaEn ? fecha(s.aprobadaEn) : "—"}</dd></div>
            <div>
              <dt>Avance de etapas</dt>
              <dd>
                <div className="progreso" title={porcentaje(s.avance)}>
                  <div style={{ width: `${Math.min(100, s.avance)}%` }} />
                </div>
                {" "}{s.etapasCompletas}/10 · {porcentaje(s.avance)}
                {s.etapasBloqueadas > 0 ? ` · ${s.etapasBloqueadas} bloqueada(s)` : ""}
              </dd>
            </div>
          </dl>
          {s.descripcion && <p>{s.descripcion}</p>}

          <div className="acciones">
            {editable && puedeEnviarAEvaluacion(s.estado) && (
              <button
                className="boton primario"
                disabled={ocupado}
                onClick={() => void mutar(() => cambiosApi.actualizar(id, datosDe(s, { estado: "EN_EVALUACION" })))}
              >
                Enviar a evaluación
              </button>
            )}
            {puedeAprobar() && puedeAprobarDesde(s.estado) && (
              <button
                className="boton primario"
                disabled={ocupado}
                onClick={() => void mutar(() => cambiosApi.aprobar(id))}
              >
                Aprobar
              </button>
            )}
            {puedeAprobar() && puedeRechazarDesde(s.estado) && (
              <button
                className="boton peligro"
                disabled={ocupado}
                onClick={() => void mutar(() => cambiosApi.rechazar(id))}
              >
                Rechazar
              </button>
            )}
            {editable && puedeAplicarAlPlan(s.estado, s.aplicada) && (
              <button
                className="boton primario"
                disabled={ocupado || faltaTareaTiempo}
                onClick={() => {
                  if (!window.confirm("¿Aplicar al plan vigente? La línea base no cambia.")) return;
                  void mutar(() => cambiosApi.aplicar(id));
                }}
              >
                Aplicar al plan
              </button>
            )}
            {editable && puedeCerrar(s.estado) && (
              <button
                className="boton"
                disabled={ocupado}
                onClick={() => void mutar(() => cambiosApi.actualizar(id, datosDe(s, { estado: "CERRADO" })))}
              >
                Cerrar
              </button>
            )}
          </div>
          {faltaTareaTiempo && puedeAplicarAlPlan(s.estado, s.aplicada) && (
            <p className="aviso">Para aplicar un cambio de tiempo hace falta una tarea hoja.</p>
          )}
          {aplicaSinGantt && (
            <p className="aviso">Este tipo no mueve el Gantt: al aplicar solo queda marcada como aplicada y pasa a En implementación.</p>
          )}

          <h3>Etapas</h3>
          <ol className="linea-etapas">
            {etapas.map((e) => (
              <li key={e.numero}>
                <span className="codigo">{e.numero}</span>
                <span>{e.nombre}</span>
                {editable ? (
                  <select
                    value={e.estado}
                    disabled={ocupado}
                    aria-label={`Estado de ${e.nombre}`}
                    onChange={(ev) => void cambiarEtapa(e.numero, ev.target.value as EstadoEtapa)}
                  >
                    {ESTADOS_ETAPA.map((est) => (
                      <option key={est} value={est}>{ETIQUETA_ETAPA[est]}</option>
                    ))}
                  </select>
                ) : (
                  <span className={`etiqueta estado-${e.estado}`}>{ETIQUETA_ETAPA[e.estado]}</span>
                )}
              </li>
            ))}
          </ol>

          <p className="vacio">Evidencias e historial de trazabilidad: C-05 (aún no hay API).</p>
        </>
      )}
    </section>
  );
}
