package cl.proyecto.gestion.planificacion.lineabase;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Totales del proyecto: base contra plan vigente y real. */
public record ResumenComparacion(
    LocalDate finBase,
    LocalDate finPlan,
    Integer desviacionFinDias,
    BigDecimal costoBase,
    BigDecimal costoPlan,
    BigDecimal costoReal,
    BigDecimal desviacionCostoPlan,
    BigDecimal desviacionCostoReal,
    BigDecimal avance) {}
