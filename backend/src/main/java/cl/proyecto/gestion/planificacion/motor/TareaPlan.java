package cl.proyecto.gestion.planificacion.motor;

import java.time.LocalDate;

/** Tarea hoja de la EDT. inicioMinimo es nulo si solo la restringen sus predecesoras. */
public record TareaPlan(long id, int duracionDias, LocalDate inicioMinimo) {}
