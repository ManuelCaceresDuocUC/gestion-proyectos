package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Edita tipo y desfase. Para cambiar las tareas se elimina la dependencia y se crea otra. */
public record DependenciaEdicion(
    @NotNull TipoDependencia tipo,
    @Min(-3650) @Max(3650) Integer desfaseDias) {}
