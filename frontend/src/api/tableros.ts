import { api } from "./client";
import type { TableroProyecto } from "../types/tablero";
import type { TableroCambio } from "../types/tableros";

export const tablerosApi = {
  proyecto: (proyectoId: number) => api<TableroProyecto>(`/tablero/proyecto/${proyectoId}`),
  cambio: (proyectoId: number) => api<TableroCambio>(`/tablero/cambio/${proyectoId}`),
};
