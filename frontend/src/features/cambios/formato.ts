import type {
  EstadoSolicitud,
  ImpactoSolicitud,
  PrioridadSolicitud,
  RiesgoSolicitud,
  TipoSolicitud,
} from "../../types";

export const ESTADOS_SOLICITUD: EstadoSolicitud[] = [
  "ABIERTO", "EN_EVALUACION", "APROBADO", "RECHAZADO", "EN_IMPLEMENTACION", "CERRADO",
];
export const TIPOS_SOLICITUD: TipoSolicitud[] = [
  "ALCANCE", "TIEMPO", "COSTO", "RECURSO", "TECNOLOGIA", "ORGANIZACION",
];
export const PRIORIDADES: PrioridadSolicitud[] = ["BAJA", "MEDIA", "ALTA", "CRITICA"];
export const RIESGOS: RiesgoSolicitud[] = ["BAJO", "MEDIO", "ALTO", "CRITICO"];
export const IMPACTOS: ImpactoSolicitud[] = ["BAJO", "MEDIO", "ALTO"];

export const ETIQUETA_ESTADO: Record<EstadoSolicitud, string> = {
  ABIERTO: "Abierto",
  EN_EVALUACION: "En evaluación",
  APROBADO: "Aprobado",
  RECHAZADO: "Rechazado",
  EN_IMPLEMENTACION: "En implementación",
  CERRADO: "Cerrado",
};

export const ETIQUETA_TIPO: Record<TipoSolicitud, string> = {
  ALCANCE: "Alcance",
  TIEMPO: "Tiempo",
  COSTO: "Costo",
  RECURSO: "Recurso",
  TECNOLOGIA: "Tecnología",
  ORGANIZACION: "Organización",
};

export const ETIQUETA_PRIORIDAD: Record<PrioridadSolicitud, string> = {
  BAJA: "Baja",
  MEDIA: "Media",
  ALTA: "Alta",
  CRITICA: "Crítica",
};

export const ETIQUETA_RIESGO: Record<RiesgoSolicitud, string> = {
  BAJO: "Bajo",
  MEDIO: "Medio",
  ALTO: "Alto",
  CRITICO: "Crítico",
};

export const ETIQUETA_IMPACTO: Record<ImpactoSolicitud, string> = {
  BAJO: "Bajo",
  MEDIO: "Medio",
  ALTO: "Alto",
};

/** "2026-11-02" -> "02-11-2026". */
export function fecha(valor: string | null): string {
  if (!valor) return "—";
  const [a, m, d] = valor.slice(0, 10).split("-");
  return `${d}-${m}-${a}`;
}

export function porcentaje(valor: number): string {
  return `${Math.round(valor)} %`;
}

export function mensajeDe(error: unknown): string {
  return error instanceof Error ? error.message : "Ocurrió un error inesperado.";
}
