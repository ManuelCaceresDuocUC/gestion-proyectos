// Tipos de la comparación base / plan / real (docs/api/linea-base.md) y del cambio de duración (docs/api/planificacion.md).
import type { TipoTarea } from "./index";
import type { CambioFecha } from "./planificacion";

export interface FilaComparacion {
  tareaId: number;
  parentId: number | null;
  tipo: TipoTarea;
  codigo: string;
  nombre: string;
  inicioBase: string | null;
  finBase: string | null;
  inicioPlan: string | null;
  finPlan: string | null;
  inicioReal: string | null;
  finReal: string | null;
  desviacionInicioDias: number | null;
  desviacionFinDias: number | null;
  desviacionFinRealDias: number | null;
  avance: number;
  costoBase: number | null;
  costoPlan: number;
  costoReal: number;
  desviacionCostoPlan: number | null;
  desviacionCostoReal: number | null;
  atrasada: boolean;
}

export interface ResumenComparacion {
  finBase: string | null;
  finPlan: string | null;
  desviacionFinDias: number | null;
  costoBase: number | null;
  costoPlan: number;
  costoReal: number;
  desviacionCostoPlan: number | null;
  desviacionCostoReal: number | null;
  avance: number;
}

export interface ComparacionPlan {
  versionLineaBase: number | null;
  fijadaEn: string | null;
  resumen: ResumenComparacion;
  tareas: FilaComparacion[];
}

export interface ResultadoTarea {
  cambios: CambioFecha[];
}
