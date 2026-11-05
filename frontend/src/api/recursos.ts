import { api } from "./client";
import type {
  Asignacion,
  AsignacionDatos,
  Recurso,
  RecursoDatos,
  TipoRecurso,
  TipoRecursoDatos,
} from "../types/recursos";

function conCuerpo(metodo: string, cuerpo?: unknown): RequestInit {
  return cuerpo === undefined
    ? { method: metodo }
    : { method: metodo, body: JSON.stringify(cuerpo) };
}

export const tiposRecursoApi = {
  listar: () => api<TipoRecurso[]>("/tipos-recurso"),

  obtener: (id: number) =>
    api<TipoRecurso>(`/tipos-recurso/${id}`),

  crear: (datos: TipoRecursoDatos) =>
    api<TipoRecurso>("/tipos-recurso", conCuerpo("POST", datos)),

  actualizar: (id: number, datos: TipoRecursoDatos) =>
    api<TipoRecurso>(`/tipos-recurso/${id}`, conCuerpo("PUT", datos)),

  eliminar: (id: number) =>
    api<void>(`/tipos-recurso/${id}`, conCuerpo("DELETE")),
};

export const recursosApi = {
  listar: (soloActivos = true) =>
    api<Recurso[]>(`/recursos?soloActivos=${soloActivos}`),

  obtener: (id: number) =>
    api<Recurso>(`/recursos/${id}`),

  crear: (datos: RecursoDatos) =>
    api<Recurso>("/recursos", conCuerpo("POST", datos)),

  actualizar: (id: number, datos: RecursoDatos) =>
    api<Recurso>(`/recursos/${id}`, conCuerpo("PUT", datos)),

  eliminar: (id: number) =>
    api<void>(`/recursos/${id}`, conCuerpo("DELETE")),
};

export const asignacionesApi = {
  listarPorTarea: (tareaId: number) =>
    api<Asignacion[]>(`/asignaciones/tarea/${tareaId}`),

  obtener: (id: number) =>
    api<Asignacion>(`/asignaciones/${id}`),

  crear: (datos: AsignacionDatos) =>
    api<Asignacion>("/asignaciones", conCuerpo("POST", datos)),

  actualizar: (id: number, datos: AsignacionDatos) =>
    api<Asignacion>(`/asignaciones/${id}`, conCuerpo("PUT", datos)),

  eliminar: (id: number) =>
    api<void>(`/asignaciones/${id}`, conCuerpo("DELETE")),
};
