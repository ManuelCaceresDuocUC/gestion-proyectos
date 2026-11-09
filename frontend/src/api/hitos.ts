import { api } from "./client";
import type { Hito, HitoDatos } from "../types/hitos";

function conCuerpo(metodo: string, cuerpo?: unknown): RequestInit {
  return cuerpo === undefined ? { method: metodo } : { method: metodo, body: JSON.stringify(cuerpo) };
}

export const hitosApi = {
  listar: (proyectoId: number) => api<Hito[]>(`/proyectos/${proyectoId}/hitos`),
  obtener: (id: number) => api<Hito>(`/hitos/${id}`),
  crear: (proyectoId: number, datos: HitoDatos) => api<Hito>(`/proyectos/${proyectoId}/hitos`, conCuerpo("POST", datos)),
  actualizar: (id: number, datos: HitoDatos) => api<Hito>(`/hitos/${id}`, conCuerpo("PUT", datos)),
  eliminar: (id: number) => api<void>(`/hitos/${id}`, conCuerpo("DELETE")),
};
