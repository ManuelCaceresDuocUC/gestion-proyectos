package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Crea una dependencia. desfaseDias en días hábiles (negativo = adelanto); nulo = 0. */
public record DependenciaRequest(
    @NotNull Long predecesoraId,
    @NotNull Long sucesoraId,
    @NotNull TipoDependencia tipo,
    @Min(-3650) @Max(3650) Integer desfaseDias) {}
