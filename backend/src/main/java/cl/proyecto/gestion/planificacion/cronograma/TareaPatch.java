package cl.proyecto.gestion.planificacion.cronograma;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Cambio parcial de una tarea hoja: los campos nulos no se tocan. Al menos uno debe venir. */
public record TareaPatch(
    @Min(0) Integer duracionDias,
    LocalDate inicioReal,
    LocalDate finReal,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal avance) {}
