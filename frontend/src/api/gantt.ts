import { api } from "./client";
import type { ComparacionPlan, ResultadoTarea } from "../types/gantt";

export const ganttApi = {
  comparacion: (proyectoId: number, hoy: string) =>
    api<ComparacionPlan>(`/proyectos/${proyectoId}/comparacion?hoy=${encodeURIComponent(hoy)}`),
  /** PATCH /tareas/{id}: cambia la duración y devuelve las fechas que se movieron al recalcular. */
  cambiarDuracion: (tareaId: number, duracionDias: number) =>
    api<ResultadoTarea>(`/tareas/${tareaId}`, { method: "PATCH", body: JSON.stringify({ duracionDias }) }),
};
