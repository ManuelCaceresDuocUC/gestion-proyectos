import { api } from "./client";
import type { TableroProyecto } from "../types/tablero";

export const tablerosApi = {
  proyecto: (proyectoId: number) => api<TableroProyecto>(`/tablero/proyecto/${proyectoId}`),
};
