import type { EstadoSolicitud } from "../../types";

/** Espejo del grafo C-08. El 409 del backend sigue siendo la verdad. */
export function puedeEnviarAEvaluacion(estado: EstadoSolicitud): boolean {
  return estado === "ABIERTO";
}

export function puedeAprobarDesde(estado: EstadoSolicitud): boolean {
  return estado === "EN_EVALUACION";
}

export function puedeRechazarDesde(estado: EstadoSolicitud): boolean {
  return estado === "EN_EVALUACION";
}

export function puedeAplicarAlPlan(estado: EstadoSolicitud, aplicada: boolean): boolean {
  return estado === "APROBADO" && !aplicada;
}

export function puedeCerrar(estado: EstadoSolicitud): boolean {
  return estado === "EN_IMPLEMENTACION";
}
