// Tipos compartidos con los DTOs del backend. Se completan en el Sprint 0 junto con docs/api/.
export type Rol = "ADMIN" | "USUARIO_PROYECTO";
export type TipoDependencia = "FS" | "SS" | "FF" | "SF";
export type TipoTarea = "ETAPA" | "ACTIVIDAD" | "SUBACTIVIDAD";
export type Semaforo = "VERDE" | "AMARILLO" | "ROJO";
export type EstadoEtapa = "PENDIENTE" | "EN_CURSO" | "COMPLETADO" | "BLOQUEADO" | "NO_APLICA";
export type EstadoSolicitud = "ABIERTO" | "EN_EVALUACION" | "APROBADO" | "RECHAZADO" | "EN_IMPLEMENTACION" | "CERRADO";
export type TipoSolicitud = "ALCANCE" | "TIEMPO" | "COSTO" | "RECURSO" | "TECNOLOGIA" | "ORGANIZACION";
export type PrioridadSolicitud = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";
export type RiesgoSolicitud = "BAJO" | "MEDIO" | "ALTO" | "CRITICO";
export type ImpactoSolicitud = "BAJO" | "MEDIO" | "ALTO";
export type ResultadoEvaluacion = "EFECTIVO" | "PARCIAL" | "NO_EFECTIVO";
