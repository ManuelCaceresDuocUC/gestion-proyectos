package cl.proyecto.gestion.planificacion.recalculo;

import java.util.List;

/** Respuesta de una operación que dispara el recálculo: qué tareas cambiaron de fechas. */
public record ResultadoRecalculo(List<CambioFecha> cambios) {}
