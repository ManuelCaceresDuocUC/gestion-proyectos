/** JSON de GET /api/tablero/cambio/{id} (C-12). */
export type AlertaTableroTipo = "ATRASO" | "BLOQUEO" | "RIESGO_CRITICO" | "KPI_ROJO";

export interface KpiTablero {
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
  semaforo: string | null;
  motivo: string | null;
  calculadoEn: string | null;
  noCalculable: boolean;
  alerta: boolean;
  alertaMotivo: string | null;
}

export interface TableroCambio {
  proyectoId: number;
  conteos: {
    total: number;
    porEstado: Record<string, number>;
    enCurso: number;
    atrasados: number;
    riesgoAlto: number;
    riesgoCritico: number;
    conBloqueo: number;
    conCorrectiva: number;
    adoptados: number;
  };
  etapas: { avancePromedio: number };
  costos: { presupuestoCerradas: number; costoRealCerradas: number };
  tiempos: { diasAtrasoTotal: number; diasAtrasoPromedio: number };
  efectividad: { efectivo: number; parcial: number; noEfectivo: number; sinDato: number };
  alertas: { tipo: AlertaTableroTipo; mensaje: string; solicitudCodigo: string | null }[];
  kpi: KpiTablero[];
}
