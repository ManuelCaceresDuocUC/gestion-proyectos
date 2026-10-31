package cl.proyecto.gestion.planificacion.recalculo;

import java.time.LocalDate;

/** Una tarea cuyas fechas plan cambiaron por un recálculo (antes y después). */
public record CambioFecha(
    Long tareaId,
    String codigo,
    String nombre,
    LocalDate inicioAntes,
    LocalDate finAntes,
    LocalDate inicioDespues,
    LocalDate finDespues) {}
