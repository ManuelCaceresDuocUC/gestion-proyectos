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