export type CategoriaKpi = "PROYECTO" | "HITOS" | "CAMBIO";
export type SentidoKpi = "MAYOR_MEJOR" | "MENOR_MEJOR";
export type OperadorMeta = "MAYOR_IGUAL" | "MENOR_IGUAL" | "MAYOR" | "MENOR" | "ENTRE";
export type SemaforoKpi = "VERDE" | "AMARILLO" | "ROJO";

/** Definición guardada. El semáforo no viene aquí: se lee del listado del proyecto. */
export interface KpiDefinicion {
  id: number;
  proyectoId: number | null;
  codigo: string;
  nombre: string;
  categoria: CategoriaKpi;
  descripcion: string | null;
  formula: string;
  unidad: string | null;
  sentido: SentidoKpi;
  operadorMeta: OperadorMeta;
  meta: number | null;
  metaMax: number | null;
  umbralVerde: number;
  umbralAmarillo: number;
  responsable: string | null;
  activo: boolean;
  variables: string[];
}

export interface KpiDefinicionDatos {
  proyectoId: number | null;
  codigo: string;
  nombre: string;
  categoria: CategoriaKpi;
  formula: string;
  unidad: string | null;
  sentido: SentidoKpi;
  operadorMeta: OperadorMeta;
  meta: number | null;
  metaMax: number | null;
  umbralVerde: number | null;
  umbralAmarillo: number | null;
  responsable: string | null;
  activo: boolean;
}

/** Último snapshot de GET /proyectos/{id}/kpi. */
export interface KpiEstado {
  id: number;
  codigo: string;
  nombre: string;
  categoria: string;
  formula: string;
  unidad: string | null;
  sentido: string;
  operadorMeta: string;
  meta: number | null;
  valor: number | null;
  cumplimiento: number | null;
  semaforo: SemaforoKpi | null;
  motivo: string | null;
  calculadoEn: string | null;
  noCalculable: boolean;
  alerta: boolean;
  alertaMotivo: string | null;
}

export interface KpiHistorico {
  id: number;
  kpiId: number;
  proyectoId: number;
  valor: number | null;
  cumplimiento: number | null;
  semaforo: SemaforoKpi | null;
  motivo: string | null;
  calculadoEn: string;
  noCalculable: boolean;
}
