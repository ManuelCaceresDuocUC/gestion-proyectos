import { api } from "./client";
import type { KpiDefinicion, KpiDefinicionDatos, KpiEstado, KpiHistorico } from "../types/kpi";

function conCuerpo(metodo: string, cuerpo?: unknown): RequestInit {
  return cuerpo === undefined ? { method: metodo } : { method: metodo, body: JSON.stringify(cuerpo) };
}

export const kpiApi = {
  definiciones: (proyectoId: number) =>
    api<KpiDefinicion[]>(`/kpi?proyectoId=${proyectoId}`),
  estados: (proyectoId: number) => api<KpiEstado[]>(`/proyectos/${proyectoId}/kpi`),
  obtener: (id: number) => api<KpiDefinicion>(`/kpi/${id}`),
  historico: (id: number, proyectoId: number) =>
    api<KpiHistorico[]>(`/kpi/${id}/historico?proyectoId=${proyectoId}`),
  crear: (datos: KpiDefinicionDatos) => api<KpiDefinicion>("/kpi", conCuerpo("POST", datos)),
  actualizar: (id: number, datos: KpiDefinicionDatos) =>
    api<KpiDefinicion>(`/kpi/${id}`, conCuerpo("PUT", datos)),
  eliminar: (id: number) => api<void>(`/kpi/${id}`, conCuerpo("DELETE")),
};
