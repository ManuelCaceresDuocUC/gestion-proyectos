import type { EstadoProyecto } from "../../types/planificacion";

export const ETIQUETA_ESTADO: Record<EstadoProyecto, string> = {
  PLANIFICADO: "Planificado",
  EN_EJECUCION: "En ejecución",
  SUSPENDIDO: "Suspendido",
  TERMINADO: "Terminado",
};

export const ESTADOS_EDITABLES: EstadoProyecto[] = ["PLANIFICADO", "EN_EJECUCION", "SUSPENDIDO"];

/** "2026-11-02" -> "02-11-2026". */
export function fecha(valor: string | null): string {
  if (!valor) return "—";
  const [a, m, d] = valor.slice(0, 10).split("-");
  return `${d}-${m}-${a}`;
}

export function dinero(valor: number, moneda: string): string {
  try {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: moneda,
      maximumFractionDigits: moneda === "CLP" ? 0 : 2,
    }).format(valor);
  } catch {
    return `${valor} ${moneda}`;
  }
}

export function porcentaje(valor: number): string {
  return `${Math.round(valor)} %`;
}

export function mensajeDe(error: unknown): string {
  return error instanceof Error ? error.message : "Ocurrió un error inesperado.";
}
