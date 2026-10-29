package cl.proyecto.gestion.proyecto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProyectoResponse(
    Long id,
    String codigo,
    String nombre,
    String descripcion,
    EstadoProyecto estado,
    Long responsableId,
    BigDecimal presupuesto,
    String moneda,
    String diasLaborales,
    LocalDate fechaInicio,
    LocalDate fechaFinPlan,
    LocalDate fechaFinBase,
    LocalDate fechaFinReal,
    BigDecimal avance,
    BigDecimal costoPlan,
    BigDecimal costoReal,
    Instant creadoEn,
    Instant cerradoEn) {

    public static ProyectoResponse de(Proyecto p) {
        return new ProyectoResponse(p.id, p.codigo, p.nombre, p.descripcion, p.estado, p.responsableId,
            p.presupuesto, p.moneda, p.diasLaborales, p.fechaInicio, p.fechaFinPlan, p.fechaFinBase,
            p.fechaFinReal, p.avance, p.costoPlan, p.costoReal, p.creadoEn, p.cerradoEn);
    }
}
