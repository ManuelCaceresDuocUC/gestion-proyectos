export type EstadoHito = "PENDIENTE" | "CUMPLIDO" | "NO_CUMPLIDO";

export interface ActividadHito {
  id: number;
  codigo: string;
  nombre: string;
  finPlan: string | null;
  finReal: string | null;
}

export interface Hito {
  id: number;
  proyectoId: number;
  codigo: string;
  logro: string;
  semanaPrevista: number;
  fechaPlan: string | null;
  fechaBase: string | null;
  fechaReal: string | null;
  estado: EstadoHito;
  evidencia: string | null;
  indicador: string | null;
  criterioAceptacion: string | null;
  validador: string | null;
  accionSiNoCumple: string | null;
  responsableId: number | null;
  tareaIds: number[];
  actividades: ActividadHito[];
}

export interface HitoDatos {
  codigo: string;
  logro: string;
  semanaPrevista: number;
  evidencia: string | null;
  indicador: string | null;
  criterioAceptacion: string | null;
  validador: string | null;
  accionSiNoCumple: string | null;
  estado: EstadoHito;
  fechaReal: string | null;
  responsableId: number | null;
  tareaIds: number[];
}
