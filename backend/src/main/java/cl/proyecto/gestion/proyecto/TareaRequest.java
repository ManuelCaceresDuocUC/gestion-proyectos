package cl.proyecto.gestion.proyecto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos editables de una tarea (POST y PUT). parentId solo se usa al crear: null crea una etapa; si no, el tipo se
 * deduce del padre (padre etapa -> actividad, padre actividad -> subactividad).
 * duracionDias nulo = 1 día hábil al crear (en PUT se mantiene el valor actual); avance nulo = 0 al crear.
 */
public record TareaRequest(
    Long parentId,
    @NotBlank @Size(max = 200) String nombre,
    String descripcion,
    @Min(0) Integer duracionDias,
    Long responsableId,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal avance) {}
