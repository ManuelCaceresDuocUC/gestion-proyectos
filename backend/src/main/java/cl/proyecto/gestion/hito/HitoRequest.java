package cl.proyecto.gestion.hito;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Ficha de la hoja «Definir hitos». La semana prevista se guarda como fecha_plan
 * (último día hábil de esa semana). Evidencia y fuente van juntas; unidad y criterio, también.
 */
public record HitoRequest(
    @NotBlank @Size(max = 10) String codigo,
    @NotBlank @Size(max = 250) String logro,
    @NotNull @Min(1) @Max(260) Integer semanaPrevista,
    String evidencia,
    String indicador,
    String criterioAceptacion,
    @Size(max = 160) String validador,
    String accionSiNoCumple,
    @NotNull EstadoHito estado,
    LocalDate fechaReal,
    Long responsableId,
    @NotEmpty List<@NotNull Long> tareaIds
) {}
