import { api } from "./client";
import type {
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