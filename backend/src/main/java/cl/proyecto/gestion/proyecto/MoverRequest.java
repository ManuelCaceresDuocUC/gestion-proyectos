package cl.proyecto.gestion.proyecto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nueva posición (1 = primera) de una tarea entre sus hermanos. */
public record MoverRequest(@NotNull @Min(1) Integer posicion) {}
