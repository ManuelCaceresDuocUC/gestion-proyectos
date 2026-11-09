// Llamadas a la API de solicitudes de cambio (docs/api/cambios.md, C-05 historial y evidencias).
import { api } from "./client";
import type {
  CambioEtapa,
  CambioHistorial,
  EtapasCambio,
  Evidencia,
  EvidenciaDatos,
  FiltrosSolicitud,
  SolicitudCambio,
  SolicitudCambioDatos,
} from "../types/cambios";
import type { EstadoEtapa } from "../types";

function conCuerpo(metodo: string, cuerpo?: unknown): RequestInit {
  return cuerpo === undefined ? { method: metodo } : { method: metodo, body: JSON.stringify(cuerpo) };
}

function queryFiltros(filtros: FiltrosSolicitud = {}): string {
  const q = new URLSearchParams();
  if (filtros.estado) q.set("estado", filtros.estado);
  if (filtros.tipo) q.set("tipo", filtros.tipo);
  if (filtros.prioridad) q.set("prioridad", filtros.prioridad);
  if (filtros.responsableId) q.set("responsableId", filtros.responsableId);
  if (filtros.desde) q.set("desde", filtros.desde);
  if (filtros.hasta) q.set("hasta", filtros.hasta);
  const texto = q.toString();
  return texto ? `?${texto}` : "";
}

export const cambiosApi = {
  listar: (proyectoId: number, filtros: FiltrosSolicitud = {}) =>
    api<SolicitudCambio[]>(`/proyectos/${proyectoId}/cambios${queryFiltros(filtros)}`),
  obtener: (id: number) => api<SolicitudCambio>(`/cambios/${id}`),
  crear: (proyectoId: number, datos: SolicitudCambioDatos) =>
    api<SolicitudCambio>(`/proyectos/${proyectoId}/cambios`, conCuerpo("POST", datos)),
  actualizar: (id: number, datos: SolicitudCambioDatos) =>
    api<SolicitudCambio>(`/cambios/${id}`, conCuerpo("PUT", datos)),
  eliminar: (id: number) => api<void>(`/cambios/${id}`, conCuerpo("DELETE")),
  etapas: (id: number) => api<EtapasCambio>(`/cambios/${id}/etapas`),
  parchearEtapa: (id: number, numero: number, estado: EstadoEtapa) =>
    api<CambioEtapa>(`/cambios/${id}/etapas/${numero}`, conCuerpo("PATCH", { estado })),
  aprobar: (id: number) => api<SolicitudCambio>(`/cambios/${id}/aprobar`, conCuerpo("POST")),
  rechazar: (id: number) => api<SolicitudCambio>(`/cambios/${id}/rechazar`, conCuerpo("POST")),
  aplicar: (id: number) => api<SolicitudCambio>(`/cambios/${id}/aplicar`, conCuerpo("POST")),
  historial: (id: number) => api<CambioHistorial[]>(`/cambios/${id}/historial`),
  evidencias: (id: number) => api<Evidencia[]>(`/cambios/${id}/evidencias`),
  crearEvidencia: (id: number, datos: EvidenciaDatos) =>
    api<Evidencia>(`/cambios/${id}/evidencias`, conCuerpo("POST", datos)),
  eliminarEvidencia: (id: number, evidenciaId: number) =>
    api<void>(`/cambios/${id}/evidencias/${evidenciaId}`, conCuerpo("DELETE")),
};
