export interface TipoRecurso {
  id: number;
  nombre: string;
  unidad: string;
}

export interface TipoRecursoDatos {
  nombre: string;
  unidad: string;
}

export interface Recurso {
  id: number;
  tipoRecursoId: number;
  nombre: string;
  tarifa: number;
  capacidadHorasDia: number | null;
  activo: boolean;
}

export interface RecursoDatos {
  tipoRecursoId: number;
  nombre: string;
  tarifa: number;
  capacidadHorasDia: number | null;
  activo?: boolean;
}

export interface Asignacion {
  id: number;
  tareaId: number;
  recursoId: number;
  cantidad: number;
  tarifa: number;
  horasSemana: number | null;
  semanas: number | null;
  costoPlan: number;
  cantidadReal: number;
  costoReal: number;
}

export interface AsignacionDatos {
  tareaId: number;
  recursoId: number;
  cantidad: number;
  tarifa?: number;
  horasSemana?: number;
  semanas?: number;
}

export interface TareaAlerta {
  id: number;
  codigo: string;
  nombre: string;
}

/** Una fila de GET /api/proyectos/{id}/sobreasignaciones. */
export interface AlertaSobreasignacion {
  id: number;
  recursoId: number;
  profesional: string;
  fecha: string;
  cargaHoras: number;
  capacidadHorasDia: number;
  excesoHoras: number;
  tareas: TareaAlerta[];
}
