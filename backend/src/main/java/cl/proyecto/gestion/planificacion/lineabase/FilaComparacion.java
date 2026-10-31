package cl.proyecto.gestion.planificacion.lineabase;

import cl.proyecto.gestion.proyecto.TipoTarea;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una tarea comparada contra su línea base. Las desviaciones en días son hábiles y positivas cuando la tarea
 * quedó después de lo comprometido en la base; las de costo son positivas cuando cuesta más. Son nulas si la
 * tarea no existía al fijar la base o no hay fecha real.
 */
public record FilaComparacion(
    Long tareaId,
    Long parentId,
    TipoTarea tipo,
    String codigo,
    String nombre,
    LocalDate inicioBase,
    LocalDate finBase,
    LocalDate inicioPlan,
    LocalDate finPlan,
    LocalDate inicioReal,
    LocalDate finReal,
    Integer desviacionInicioDias,
    Integer desviacionFinDias,
    Integer desviacionFinRealDias,
    BigDecimal avance,
    BigDecimal costoBase,
    BigDecimal costoPlan,
    BigDecimal costoReal,
    BigDecimal desviacionCostoPlan,
    BigDecimal desviacionCostoReal,
    boolean atrasada) {}
