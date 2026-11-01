// Tipos de la API de solicitudes (docs/api/cambios.md).
import type {
  EstadoSolicitud,
  ImpactoSolicitud,
  PrioridadSolicitud,
  ResultadoEvaluacion,
  RiesgoSolicitud,
  TipoSolicitud,
} from "./index";

/**
 * JSON de GET/POST/PUT. Fechas en ISO (`yyyy-MM-dd`); en pantalla se muestran `dd-mm-yyyy`
 * (igual que proyectos: función `fecha`). diasAtraso y avance se calculan en el servidor.
 */
export interface SolicitudCambio {
  id: number;
  proyectoId: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  solicitante: string | null;
  responsableId: number | null;
  fechaSolicitud: string;
  inicioPrevisto: string | null;
  finPrevisto: string | null;
  finReal: string | null;
  prioridad: PrioridadSolicitud;
  tipo: TipoSolicitud;
  riesgo: RiesgoSolicitud;
  impacto: ImpactoSolicitud;
  estado: EstadoSolicitud;
  aprobadaEn: string | null;
  tareaId: number | null;
  impactoDias: number;
  impactoCosto: number;
  impactoAlcance: string | null;
  impactoRecursos: string | null;
  resultadoEvaluacion: ResultadoEvaluacion | null;
  aplicada: boolean;
  aplicadaEn: string | null;
  presupuestoPrevisto: number | null;
  costoReal: number | null;
  accionCorrectiva: string | null;
  resultado: string | null;
  creadoEn: string;
  diasAtraso: number;
  etapasCompletas: number;
  avance: number;
  etapasBloqueadas: number;
}

/** Cuerpo de POST/PUT. codigo, aplicada y proyectoId no se envían. */
export interface SolicitudCambioDatos {
  nombre: string;
  tipo: TipoSolicitud;
  descripcion?: string | null;
  solicitante?: string | null;
  responsableId?: number | null;
  fechaSolicitud?: string | null;
  inicioPrevisto?: string | null;
  finPrevisto?: string | null;
  finReal?: string | null;
  prioridad?: PrioridadSolicitud | null;
  riesgo?: RiesgoSolicitud | null;
  impacto?: ImpactoSolicitud | null;
  estado?: EstadoSolicitud | null;
  tareaId?: number | null;
  impactoDias?: number | null;
  impactoCosto?: number | null;
  impactoAlcance?: string | null;
  impactoRecursos?: string | null;
  resultadoEvaluacion?: ResultadoEvaluacion | null;
  presupuestoPrevisto?: number | null;
  costoReal?: number | null;
  accionCorrectiva?: string | null;
  resultado?: string | null;
}

/** Query de GET /proyectos/{id}/cambios. Cada campo vacío se omite. */
export interface FiltrosSolicitud {
  estado?: EstadoSolicitud | "";
  tipo?: TipoSolicitud | "";
  prioridad?: PrioridadSolicitud | "";
  responsableId?: string;
  desde?: string;
  hasta?: string;
}
