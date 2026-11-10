import { api } from "./client";
import type { TableroCambio } from "../types/tableros";

export const tablerosApi = {
  cambio: (proyectoId: number) => api<TableroCambio>(`/tablero/cambio/${proyectoId}`),
};
