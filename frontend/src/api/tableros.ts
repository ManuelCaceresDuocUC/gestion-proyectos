import { api } from "./client";
import type { TableroProyecto } from "../types/tablero";
import type { TableroCambio } from "../types/tableros";

export type FiltrosTableroProyecto = {
  etapaId?: string;
  desde?: string;
  hasta?: string;
  responsableId?: string;
};

export type FiltrosTableroCambio = {
  estado?: string;
  prioridad?: string;
  riesgo?: string;
  responsableId?: string;
};

function query(params: Record<string, string | undefined>): string {
  const q = new URLSearchParams();
  for (const [clave, valor] of Object.entries(params)) {
    if (valor) q.set(clave, valor);
  }
  const texto = q.toString();
  return texto ? `?${texto}` : "";
}

export const tablerosApi = {
  proyecto: (proyectoId: number, filtros: FiltrosTableroProyecto = {}) =>
    api<TableroProyecto>(`/tablero/proyecto/${proyectoId}${query({
      etapaId: filtros.etapaId,
      desde: filtros.desde,
      hasta: filtros.hasta,
      responsableId: filtros.responsableId,
    })}`),
  cambio: (proyectoId: number, filtros: FiltrosTableroCambio = {}) =>
    api<TableroCambio>(`/tablero/cambio/${proyectoId}${query({
      estado: filtros.estado,
      prioridad: filtros.prioridad,
      riesgo: filtros.riesgo,
      responsableId: filtros.responsableId,
    })}`),
};
