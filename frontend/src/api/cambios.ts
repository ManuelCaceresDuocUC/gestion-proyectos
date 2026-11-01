// Llamadas a la API de solicitudes de cambio (docs/api/cambios.md).
import { api } from "./client";
import type { FiltrosSolicitud, SolicitudCambio, SolicitudCambioDatos } from "../types/cambios";

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
};
