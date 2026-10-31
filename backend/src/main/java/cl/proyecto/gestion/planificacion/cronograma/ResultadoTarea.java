package cl.proyecto.gestion.planificacion.cronograma;

import cl.proyecto.gestion.planificacion.recalculo.CambioFecha;
import cl.proyecto.gestion.proyecto.TareaResponse;
import java.util.List;

/** Tarea modificada y las tareas cuyas fechas se movieron como consecuencia. */
public record ResultadoTarea(TareaResponse tarea, List<CambioFecha> cambios) {}
