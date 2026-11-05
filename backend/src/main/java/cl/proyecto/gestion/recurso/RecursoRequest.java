package cl.proyecto.gestion.recurso;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RecursoRequest(
    @NotNull Long tipoRecursoId,
    @NotBlank @Size(max = 160) String nombre,
    @NotNull @DecimalMin("0.0") BigDecimal tarifa,
    @DecimalMin("0.0") BigDecimal capacidadHorasDia,
    Boolean activo) {}