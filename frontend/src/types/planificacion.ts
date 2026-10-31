// Tipos de la API de proyectos, EDT, dependencias y línea base (docs/api/proyectos-edt.md, planificacion.md, linea-base.md).
import type { TipoDependencia, TipoTarea } from "./index";

export type EstadoProyecto = "PLANIFICADO" | "EN_EJECUCION" | "SUSPENDIDO" | "TERMINADO";

export interface Proyecto {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  estado: EstadoProyecto;
  responsableId: number | null;
  presupuesto: number;
  moneda: string;
  diasLaborales: string;
  fechaInicio: string | null;
  fechaFinPlan: string | null;
  fechaFinBase: string | null;
  fechaFinReal: string | null;
  avance: number;
  costoPlan: number;
  costoReal: number;
  creadoEn: string;
  cerradoEn: string | null;
}

/** Cuerpo de POST/PUT /proyectos. */
export interface ProyectoDatos {
  codigo: string;
  nombre: string;
  descripcion: string | null;
  estado: EstadoProyecto;
  responsableId: number | null;
  presupuesto: number;
  moneda: string;
  diasLaborales: string;
  fechaInicio: string | null;
}

export interface NodoEdt {
  id: number;
  parentId: number | null;
  tipo: TipoTarea;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  orden: number;
  responsableId: number | null;
  duracionDias: number | null;
  inicioPlan: string | null;
  finPlan: string | null;
  avance: number;
  costoPlan: number;
  costoReal: number;
  hijos: NodoEdt[];
}

/** Cuerpo de POST /proyectos/{id}/tareas y PUT /tareas/{id}. */
export interface TareaDatos {
  parentId?: number | null;
  nombre: string;
  descripcion: string | null;
  duracionDias?: number | null;
  responsableId: number | null;
  avance?: number | null;
}

export interface DependenciaDto {
  id: number;
  predecesoraId: number;
  sucesoraId: number;
  tipo: TipoDependencia;
  desfaseDias: number;
}

export interface CambioFecha {
  tareaId: number;
  codigo: string;
  nombre: string;
  inicioAntes: string | null;
  finAntes: string | null;
  inicioDespues: string | null;
  finDespues: string | null;
}

export interface ResultadoDependencia {
  dependencia: DependenciaDto;
  cambios: CambioFecha[];
}

export interface ResultadoRecalculo {
  cambios: CambioFecha[];
}

export interface LineaBaseDto {
  id: number;
  proyectoId: number;
  version: number;
  motivo: string | null;
  creadaEn: string;
  fechaFin: string | null;
  costoTotal: number;
}
