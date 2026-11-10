import type { AlertaSobreasignacion } from "./recursos";

/** GET /api/tablero/proyecto/{id}. Nombres de docs/api/tableros.md. */
export interface TableroProyecto {
  proyectoId: number;
  codigo: string;
  nombre: string;
  avance: { real: number; planificado: number };
  hitos: {
    cumplidos: HitoTablero[];
    proximos: HitoTablero[];
    vencidos: HitoTablero[];
  };
  costos: { plan: number; real: number; presupuesto: number; moneda: string };
  recursos: {
    profesionales: number;
    sobreasignados: number;
    alertas: AlertaSobreasignacion[];
  };
  actividadesAtrasadas: ActividadAtrasada[];
  kpi: KpiTablero[];
}

export interface HitoTablero {
  id: number;
  codigo: string;
  logro: string;
  fechaPlan: string | null;
  fechaReal: string | null;
  estado: string;
}

export interface ActividadAtrasada {
  id: number;
  codigo: string;
  nombre: string;
  finPlan: string;
  avance: number;
}

export interface KpiTablero {
  id: number;
  codigo: string;
  nombre: string;
  categoria: "PROYECTO" | "HITOS" | string;
  formula: string;
  unidad: string | null;
  sentido: string;
  operadorMeta: string;
  meta: number | null;
  valor: number | null;
  cumplimiento: number | null;
  semaforo: "VERDE" | "AMARILLO" | "ROJO" | null;
  motivo: string | null;
  calculadoEn: string | null;
  noCalculable: boolean;
  alerta: boolean;
  alertaMotivo: string | null;
}
