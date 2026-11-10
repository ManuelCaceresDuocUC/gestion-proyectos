import type { EstadoSolicitud } from "../../types";
import type { NodoEdt } from "../../types/planificacion";
import type { AlertaTableroTipo } from "../../types/tableros";
import { ESTADOS_SOLICITUD, ETIQUETA_ESTADO } from "../cambios/formato";

export { ESTADOS_SOLICITUD, ETIQUETA_ESTADO };

export const ETIQUETA_ALERTA: Record<AlertaTableroTipo, string> = {
  ATRASO: "Atraso",
  BLOQUEO: "Bloqueo",
  RIESGO_CRITICO: "Riesgo crítico",
  KPI_ROJO: "KPI en rojo",
};

export const ETIQUETA_SEMAFORO: Record<string, string> = {
  VERDE: "Verde",
  AMARILLO: "Amarillo",
  ROJO: "Rojo",
};

export function cuentaEstado(porEstado: Record<string, number>, estado: EstadoSolicitud): number {
  return porEstado[estado] ?? 0;
}

export function textoSemaforo(semaforo: string | null, noCalculable: boolean): string {
  if (noCalculable || !semaforo) return "No calculable";
  return ETIQUETA_SEMAFORO[semaforo] ?? semaforo;
}

export function textoValorKpi(valor: number | null, unidad: string | null, noCalculable: boolean): string {
  if (noCalculable || valor === null) return "No calculable";
  const cifra = new Intl.NumberFormat("es-CL", { maximumFractionDigits: 2 }).format(valor);
  return unidad ? `${cifra} ${unidad}` : cifra;
}

export function textoCumplimiento(cumplimiento: number | null): string {
  if (cumplimiento === null) return "—";
  return `${Math.round(cumplimiento * 100)} %`;
}

export function decimales(valor: number): string {
  return new Intl.NumberFormat("es-CL", { maximumFractionDigits: 2 }).format(valor);
}

/** Etapas de la EDT (filtro C-13). */
export function etapasDe(nodos: NodoEdt[]): NodoEdt[] {
  const etapas: NodoEdt[] = [];
  for (const nodo of nodos) {
    if (nodo.tipo === "ETAPA") etapas.push(nodo);
    if (nodo.hijos.length > 0) etapas.push(...etapasDe(nodo.hijos));
  }
  return etapas;
}
