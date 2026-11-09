import type {
  EstadoEtapa,
  EstadoSolicitud,
  ImpactoSolicitud,
  PrioridadSolicitud,
  RiesgoSolicitud,
  TipoSolicitud,
} from "../../types";
import type { SolicitudCambio, SolicitudCambioDatos } from "../../types/cambios";

export const ESTADOS_SOLICITUD: EstadoSolicitud[] = [
  "ABIERTO", "EN_EVALUACION", "APROBADO", "RECHAZADO", "EN_IMPLEMENTACION", "CERRADO",
];
export const TIPOS_SOLICITUD: TipoSolicitud[] = [
  "ALCANCE", "TIEMPO", "COSTO", "RECURSO", "TECNOLOGIA", "ORGANIZACION",
];
export const PRIORIDADES: PrioridadSolicitud[] = ["BAJA", "MEDIA", "ALTA", "CRITICA"];
export const RIESGOS: RiesgoSolicitud[] = ["BAJO", "MEDIO", "ALTO", "CRITICO"];
export const IMPACTOS: ImpactoSolicitud[] = ["BAJO", "MEDIO", "ALTO"];
export const ESTADOS_ETAPA: EstadoEtapa[] = [
  "PENDIENTE", "EN_CURSO", "COMPLETADO", "BLOQUEADO", "NO_APLICA",
];

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

export const ETIQUETA_ETAPA: Record<EstadoEtapa, string> = {
  PENDIENTE: "Pendiente",
  EN_CURSO: "En curso",
  COMPLETADO: "Completado",
  BLOQUEADO: "Bloqueado",
  NO_APLICA: "No aplica",
};

/** "2026-11-02" -> "02-11-2026". */
export function fecha(valor: string | null): string {
  if (!valor) return "—";
  const [a, m, d] = valor.slice(0, 10).split("-");
  return `${d}-${m}-${a}`;
}

/** Instant ISO → fecha y hora en America/Santiago. */
export function fechaHora(valor: string | null): string {
  if (!valor) return "—";
  const d = new Date(valor);
  if (Number.isNaN(d.getTime())) return "—";
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: "America/Santiago",
  }).format(d);
}

/** `estado` o `etapa.{n}.estado` como los escribe el servidor. */
export function etiquetaCampoHistorial(campo: string): string {
  if (campo === "estado") return "Estado";
  const etapa = /^etapa\.(\d+)\.estado$/.exec(campo);
  return etapa ? `Etapa ${etapa[1]}` : campo;
}

export function etiquetaValorHistorial(campo: string, valor: string | null): string {
  if (!valor) return "—";
  if (campo === "estado" && valor in ETIQUETA_ESTADO) {
    return ETIQUETA_ESTADO[valor as EstadoSolicitud];
  }
  if (campo.startsWith("etapa.") && valor in ETIQUETA_ETAPA) {
    return ETIQUETA_ETAPA[valor as EstadoEtapa];
  }
  return valor;
}

/** Solo http(s) se abren; el resto se muestra como texto. */
export function enlaceHttp(url: string | null): string | null {
  if (!url) return null;
  try {
    const u = new URL(url.trim());
    return u.protocol === "http:" || u.protocol === "https:" ? u.href : null;
  } catch {
    return null;
  }
}

export function porcentaje(valor: number): string {
  return `${Math.round(valor)} %`;
}

export function mensajeDe(error: unknown): string {
  return error instanceof Error ? error.message : "Ocurrió un error inesperado.";
}

export function dinero(valor: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0,
  }).format(valor);
}

/** Cuerpo de PUT con todos los campos. El PUT no es PATCH: lo omitido queda null. */
export function datosDe(s: SolicitudCambio, extra: Partial<SolicitudCambioDatos> = {}): SolicitudCambioDatos {
  return {
    nombre: s.nombre,
    tipo: s.tipo,
    descripcion: s.descripcion,
    solicitante: s.solicitante,
    responsableId: s.responsableId,
    fechaSolicitud: s.fechaSolicitud,
    inicioPrevisto: s.inicioPrevisto,
    finPrevisto: s.finPrevisto,
    finReal: s.finReal,
    prioridad: s.prioridad,
    riesgo: s.riesgo,
    impacto: s.impacto,
    estado: s.estado,
    tareaId: s.tareaId,
    impactoDias: s.impactoDias,
    impactoCosto: s.impactoCosto,
    impactoAlcance: s.impactoAlcance,
    impactoRecursos: s.impactoRecursos,
    resultadoEvaluacion: s.resultadoEvaluacion,
    presupuestoPrevisto: s.presupuestoPrevisto,
    costoReal: s.costoReal,
    accionCorrectiva: s.accionCorrectiva,
    resultado: s.resultado,
    ...extra,
  };
}
