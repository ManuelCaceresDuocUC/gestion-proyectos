package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.planificacion.recalculo.CambioFecha;
import java.util.List;

/** Dependencia guardada y las tareas cuyas fechas se movieron por ella. */
public record ResultadoDependencia(DependenciaResponse dependencia, List<CambioFecha> cambios) {}
