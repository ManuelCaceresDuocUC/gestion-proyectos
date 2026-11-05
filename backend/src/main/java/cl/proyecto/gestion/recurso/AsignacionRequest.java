package cl.proyecto.gestion.recurso;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AsignacionRequest(
    @NotNull Long tareaId,
    @NotNull Long recursoId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal cantidad,
    @DecimalMin("0.0") BigDecimal tarifa,
    @DecimalMin("0.0") BigDecimal horasSemana,
    @DecimalMin("0.0") BigDecimal semanas) {}