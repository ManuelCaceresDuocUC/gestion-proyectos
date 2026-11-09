import type { EstadoHito } from "../../types/hitos";

export const ESTADOS_HITO: EstadoHito[] = ["PENDIENTE", "CUMPLIDO", "NO_CUMPLIDO"];

export const ETIQUETA_ESTADO_HITO: Record<EstadoHito, string> = {
  PENDIENTE: "Pendiente",
  CUMPLIDO: "Cumplido",
  NO_CUMPLIDO: "No cumplido",
};
