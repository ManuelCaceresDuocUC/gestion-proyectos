package cl.proyecto.gestion.recurso;

import java.math.BigDecimal;

public record AsignacionResponse(
    Long id,
    Long tareaId,
    Long recursoId,
    BigDecimal cantidad,
    BigDecimal tarifa,
    BigDecimal horasSemana,
    BigDecimal semanas,
    BigDecimal costoPlan,
    BigDecimal cantidadReal,
    BigDecimal costoReal) {

    public static AsignacionResponse de(Asignacion asignacion) {
        return new AsignacionResponse(
            asignacion.id,
            asignacion.tareaId,
            asignacion.recursoId,
            asignacion.cantidad,
            asignacion.tarifa,
            asignacion.horasSemana,
            asignacion.semanas,
            asignacion.costoPlan,
            asignacion.cantidadReal,
            asignacion.costoReal);
    }
}