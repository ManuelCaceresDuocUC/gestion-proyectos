import type { CategoriaKpi, KpiDefinicion, KpiDefinicionDatos, OperadorMeta, SentidoKpi } from "../../types/kpi";

export const CATEGORIAS: CategoriaKpi[] = ["PROYECTO", "HITOS", "CAMBIO"];
export const SENTIDOS: SentidoKpi[] = ["MAYOR_MEJOR", "MENOR_MEJOR"];
export const OPERADORES: OperadorMeta[] = ["MAYOR_IGUAL", "MENOR_IGUAL", "MAYOR", "MENOR", "ENTRE"];

export const ETIQUETA_CATEGORIA: Record<CategoriaKpi, string> = {
  PROYECTO: "Proyecto",
  HITOS: "Hitos",
  CAMBIO: "Cambio",
};

export const ETIQUETA_SENTIDO: Record<SentidoKpi, string> = {
  MAYOR_MEJOR: "Mayor es mejor",
  MENOR_MEJOR: "Menor es mejor",
};

export const ETIQUETA_OPERADOR: Record<OperadorMeta, string> = {
  MAYOR_IGUAL: "Mayor o igual",
  MENOR_IGUAL: "Menor o igual",
  MAYOR: "Mayor",
  MENOR: "Menor",
  ENTRE: "Entre",
};

/** Nombres de docs/api/kpi-variables.md. El cálculo no se hace en el navegador. */
export const VARIABLES_PROYECTO = [
  "AVANCE_REAL",
  "AVANCE_PLAN",
  "VALOR_PLANIFICADO",
  "VALOR_GANADO",
  "COSTO_REAL",
  "COSTO_PLAN_A_LA_FECHA",
  "DIAS_ATRASO_PROYECTO",
  "ACTIVIDADES_TOTAL",
  "ACTIVIDADES_ATRASADAS",
  "HITOS_VENCIDOS",
  "HITOS_CUMPLIDOS_A_TIEMPO",
  "PROFESIONALES_TOTAL",
  "PROFESIONALES_SOBREASIGNADOS",
];

export const VARIABLES_CAMBIO = [
  "CAMBIOS_TOTAL",
  "CAMBIOS_CERRADOS",
  "CAMBIOS_CERRADOS_EN_PLAZO",
  "CAMBIOS_RIESGO_ALTO",
  "CAMBIOS_PRIORIDAD_ALTA",
  "CAMBIOS_CON_ATRASO",
  "CAMBIOS_CON_BLOQUEO",
  "CAMBIOS_CON_CORRECTIVA",
  "CAMBIOS_CERRADOS_CON_SOBRECOSTO",
  "CAMBIOS_CERRADOS_CON_PRESUPUESTO",
  "ETAPAS_COMPLETADAS",
  "ETAPAS_TOTALES",
  "CAMBIOS_ADOPTADOS",
  "COSTO_REAL_CAMBIOS_CERRADOS",
  "PRESUPUESTO_CAMBIOS_CERRADOS",
  "DIAS_ATRASO_CAMBIOS",
  "CAMBIOS_APROBADOS",
  "CAMBIOS_APROBADOS_APLICADOS",
];

export function variablesDe(categoria: CategoriaKpi): string[] {
  return categoria === "CAMBIO" ? VARIABLES_CAMBIO : VARIABLES_PROYECTO;
}

export function datosDe(definicion: KpiDefinicion, activo = definicion.activo): KpiDefinicionDatos {
  return {
    proyectoId: definicion.proyectoId,
    codigo: definicion.codigo,
    nombre: definicion.nombre,
    categoria: definicion.categoria,
    formula: definicion.formula,
    unidad: definicion.unidad,
    sentido: definicion.sentido,
    operadorMeta: definicion.operadorMeta,
    meta: definicion.meta,
    metaMax: definicion.operadorMeta === "ENTRE" ? definicion.metaMax : null,
    umbralVerde: definicion.umbralVerde,
    umbralAmarillo: definicion.umbralAmarillo,
    responsable: definicion.responsable,
    activo,
  };
}
