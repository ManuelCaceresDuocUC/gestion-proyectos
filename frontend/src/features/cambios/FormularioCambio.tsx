import { useState } from "react";
import type { FormEvent } from "react";
import { cambiosApi } from "../../api/cambios";
import type { SolicitudCambio, SolicitudCambioDatos } from "../../types/cambios";
import type { EstadoSolicitud, ImpactoSolicitud, PrioridadSolicitud, RiesgoSolicitud, TipoSolicitud } from "../../types";
import {
  ESTADOS_SOLICITUD,
  ETIQUETA_ESTADO,
  ETIQUETA_IMPACTO,
  ETIQUETA_PRIORIDAD,
  ETIQUETA_RIESGO,
  ETIQUETA_TIPO,
  IMPACTOS,
  PRIORIDADES,
  RIESGOS,
  TIPOS_SOLICITUD,
  mensajeDe,
} from "./formato";
import { puedeEditar } from "./permisos";

interface Props {
  proyectoId: number;
  solicitud?: SolicitudCambio;
  alGuardar: (guardada: SolicitudCambio) => void;
  alCancelar: () => void;
}

function iso(valor: string | null | undefined): string {
  return valor ? valor.slice(0, 10) : "";
}

function numOpcional(texto: string): number | null {
  const t = texto.trim();
  if (!t) return null;
  const n = Number(t);
  return Number.isFinite(n) ? n : null;
}

/** Crear o editar una solicitud (C-06). El PUT reenvía nombre y tipo. */
export default function FormularioCambio({ proyectoId, solicitud, alGuardar, alCancelar }: Props) {
  const [nombre, setNombre] = useState(solicitud?.nombre ?? "");
  const [tipo, setTipo] = useState<TipoSolicitud>(solicitud?.tipo ?? "TIEMPO");
  const [descripcion, setDescripcion] = useState(solicitud?.descripcion ?? "");
  const [solicitante, setSolicitante] = useState(solicitud?.solicitante ?? "");
  const [responsable, setResponsable] = useState(solicitud?.responsableId ? String(solicitud.responsableId) : "");
  const [fechaSolicitud, setFechaSolicitud] = useState(iso(solicitud?.fechaSolicitud));
  const [inicioPrevisto, setInicioPrevisto] = useState(iso(solicitud?.inicioPrevisto));
  const [finPrevisto, setFinPrevisto] = useState(iso(solicitud?.finPrevisto));
  const [finReal, setFinReal] = useState(iso(solicitud?.finReal));
  const [prioridad, setPrioridad] = useState<PrioridadSolicitud>(solicitud?.prioridad ?? "MEDIA");
  const [riesgo, setRiesgo] = useState<RiesgoSolicitud>(solicitud?.riesgo ?? "MEDIO");
  const [impacto, setImpacto] = useState<ImpactoSolicitud>(solicitud?.impacto ?? "MEDIO");
  const [estado, setEstado] = useState<EstadoSolicitud>(solicitud?.estado ?? "ABIERTO");
  const [impactoDias, setImpactoDias] = useState(solicitud ? String(solicitud.impactoDias) : "0");
  const [impactoCosto, setImpactoCosto] = useState(solicitud ? String(solicitud.impactoCosto) : "0");
  const [impactoAlcance, setImpactoAlcance] = useState(solicitud?.impactoAlcance ?? "");
  const [impactoRecursos, setImpactoRecursos] = useState(solicitud?.impactoRecursos ?? "");
  const [presupuesto, setPresupuesto] = useState(
    solicitud?.presupuestoPrevisto != null ? String(solicitud.presupuestoPrevisto) : "",
  );
  const [costoReal, setCostoReal] = useState(solicitud?.costoReal != null ? String(solicitud.costoReal) : "");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [borrando, setBorrando] = useState(false);

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    const datos: SolicitudCambioDatos = {
      nombre: nombre.trim(),
      tipo,
      descripcion: descripcion.trim() || null,
      solicitante: solicitante.trim() || null,
      responsableId: numOpcional(responsable),
      fechaSolicitud: fechaSolicitud || null,
      inicioPrevisto: inicioPrevisto || null,
      finPrevisto: finPrevisto || null,
      finReal: finReal || null,
      prioridad,
      riesgo,
      impacto,
      estado: solicitud ? estado : null,
      impactoDias: numOpcional(impactoDias) ?? 0,
      impactoCosto: numOpcional(impactoCosto) ?? 0,
      impactoAlcance: impactoAlcance.trim() || null,
      impactoRecursos: impactoRecursos.trim() || null,
      presupuestoPrevisto: numOpcional(presupuesto),
      costoReal: numOpcional(costoReal),
    };
    setGuardando(true);
    try {
      const guardada = solicitud
        ? await cambiosApi.actualizar(solicitud.id, datos)
        : await cambiosApi.crear(proyectoId, datos);
      alGuardar(guardada);
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  async function borrar() {
    if (!solicitud || !window.confirm(`¿Eliminar ${solicitud.codigo}?`)) {
      return;
    }
    setError("");
    setBorrando(true);
    try {
      await cambiosApi.eliminar(solicitud.id);
      alGuardar(solicitud);
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setBorrando(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h3>{solicitud ? `Editar ${solicitud.codigo}` : "Nueva solicitud"}</h3>
      <div className="rejilla">
        <label>Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={200} />
        </label>
        <label>Tipo
          <select value={tipo} onChange={(e) => setTipo(e.target.value as TipoSolicitud)} required>
            {TIPOS_SOLICITUD.map((s) => <option key={s} value={s}>{ETIQUETA_TIPO[s]}</option>)}
          </select>
        </label>
        {solicitud && (
          <label>Estado
            <select value={estado} onChange={(e) => setEstado(e.target.value as EstadoSolicitud)}>
              {ESTADOS_SOLICITUD.map((s) => <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>)}
            </select>
          </label>
        )}
        <label>Prioridad
          <select value={prioridad} onChange={(e) => setPrioridad(e.target.value as PrioridadSolicitud)}>
            {PRIORIDADES.map((s) => <option key={s} value={s}>{ETIQUETA_PRIORIDAD[s]}</option>)}
          </select>
        </label>
        <label>Riesgo
          <select value={riesgo} onChange={(e) => setRiesgo(e.target.value as RiesgoSolicitud)}>
            {RIESGOS.map((s) => <option key={s} value={s}>{ETIQUETA_RIESGO[s]}</option>)}
          </select>
        </label>
        <label>Impacto
          <select value={impacto} onChange={(e) => setImpacto(e.target.value as ImpactoSolicitud)}>
            {IMPACTOS.map((s) => <option key={s} value={s}>{ETIQUETA_IMPACTO[s]}</option>)}
          </select>
        </label>
        <label>Solicitante
          <input value={solicitante} onChange={(e) => setSolicitante(e.target.value)} maxLength={120} />
        </label>
        <label>Responsable (id)
          <input type="number" min={1} value={responsable} onChange={(e) => setResponsable(e.target.value)} />
        </label>
        <label>Fecha de solicitud
          <input type="date" value={fechaSolicitud} onChange={(e) => setFechaSolicitud(e.target.value)} />
        </label>
        <label>Inicio previsto
          <input type="date" value={inicioPrevisto} onChange={(e) => setInicioPrevisto(e.target.value)} />
        </label>
        <label>Fin previsto
          <input type="date" value={finPrevisto} onChange={(e) => setFinPrevisto(e.target.value)} />
        </label>
        <label>Fin real
          <input type="date" value={finReal} onChange={(e) => setFinReal(e.target.value)} />
        </label>
        <label>Impacto (días)
          <input type="number" value={impactoDias} onChange={(e) => setImpactoDias(e.target.value)} />
        </label>
        <label>Impacto costo
          <input type="number" min={0} step="any" value={impactoCosto} onChange={(e) => setImpactoCosto(e.target.value)} />
        </label>
        <label>Presupuesto previsto
          <input type="number" min={0} step="any" value={presupuesto} onChange={(e) => setPresupuesto(e.target.value)} />
        </label>
        <label>Costo real
          <input type="number" min={0} step="any" value={costoReal} onChange={(e) => setCostoReal(e.target.value)} />
        </label>
      </div>
      <label>Descripción
        <textarea rows={2} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
      </label>
      <label>Impacto en alcance
        <textarea rows={2} value={impactoAlcance} onChange={(e) => setImpactoAlcance(e.target.value)} />
      </label>
      <label>Impacto en recursos
        <textarea rows={2} value={impactoRecursos} onChange={(e) => setImpactoRecursos(e.target.value)} />
      </label>
      {!solicitud && (
        <p className="aviso">Al guardar se crea en estado Abierto, con las 10 etapas en Pendiente. El código (SC-01…) lo asigna el servidor.</p>
      )}
      {error && <p className="error" role="alert">{error}</p>}
      <div className="acciones">
        <button type="submit" className="boton primario" disabled={guardando || borrando}>
          {guardando ? "Guardando…" : "Guardar"}
        </button>
        <button type="button" className="boton" onClick={alCancelar} disabled={guardando || borrando}>Cancelar</button>
        {solicitud && puedeEditar() && (
          <button type="button" className="boton" onClick={borrar} disabled={guardando || borrando}>
            {borrando ? "Eliminando…" : "Eliminar"}
          </button>
        )}
      </div>
    </form>
  );
}
