// Llamadas a la API de proyectos, EDT, dependencias y línea base.
import { api } from "./client";
import type {
  CambioFecha,
  DependenciaDto,
  LineaBaseDto,
  NodoEdt,
  Proyecto,
  ProyectoDatos,
  ResultadoDependencia,
  ResultadoRecalculo,
  TareaDatos,
} from "../types/planificacion";
import type { TipoDependencia } from "../types";

function conCuerpo(metodo: string, cuerpo?: unknown): RequestInit {
  return cuerpo === undefined ? { method: metodo } : { method: metodo, body: JSON.stringify(cuerpo) };
}

export const proyectosApi = {
  listar(estado: string, responsableId: string): Promise<Proyecto[]> {
    const q = new URLSearchParams();
    if (estado) q.set("estado", estado);
    if (responsableId) q.set("responsableId", responsableId);
    const texto = q.toString();
    return api<Proyecto[]>(`/proyectos${texto ? `?${texto}` : ""}`);
  },
  obtener: (id: number) => api<Proyecto>(`/proyectos/${id}`),
  crear: (datos: ProyectoDatos) => api<Proyecto>("/proyectos", conCuerpo("POST", datos)),
  actualizar: (id: number, datos: ProyectoDatos) => api<Proyecto>(`/proyectos/${id}`, conCuerpo("PUT", datos)),
  cerrar: (id: number) => api<Proyecto>(`/proyectos/${id}/cerrar`, conCuerpo("POST")),
  eliminar: (id: number) => api<void>(`/proyectos/${id}`, conCuerpo("DELETE")),
  fijarLineaBase: (id: number, motivo: string) =>
    api<LineaBaseDto>(`/proyectos/${id}/linea-base`, conCuerpo("POST", { motivo: motivo || null })),
};

export const edtApi = {
  arbol: (proyectoId: number) => api<NodoEdt[]>(`/proyectos/${proyectoId}/edt`),
  crear: (proyectoId: number, datos: TareaDatos) => api<NodoEdt>(`/proyectos/${proyectoId}/tareas`, conCuerpo("POST", datos)),
  actualizar: (id: number, datos: TareaDatos) => api<NodoEdt>(`/tareas/${id}`, conCuerpo("PUT", datos)),
  eliminar: (id: number) => api<void>(`/tareas/${id}`, conCuerpo("DELETE")),
  mover: (id: number, posicion: number) => api<void>(`/tareas/${id}/mover`, conCuerpo("POST", { posicion })),
};

export const dependenciasApi = {
  listar: (proyectoId: number) => api<DependenciaDto[]>(`/proyectos/${proyectoId}/dependencias`),
  crear: (proyectoId: number, predecesoraId: number, sucesoraId: number, tipo: TipoDependencia, desfaseDias: number) =>
    api<ResultadoDependencia>(
      `/proyectos/${proyectoId}/dependencias`,
      conCuerpo("POST", { predecesoraId, sucesoraId, tipo, desfaseDias }),
    ),
  actualizar: (id: number, tipo: TipoDependencia, desfaseDias: number) =>
    api<ResultadoDependencia>(`/dependencias/${id}`, conCuerpo("PUT", { tipo, desfaseDias })),
  eliminar: (id: number) => api<ResultadoRecalculo>(`/dependencias/${id}`, conCuerpo("DELETE")),
};

export type { CambioFecha };
