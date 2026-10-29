package cl.proyecto.gestion.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TareaResponse(
    Long id,
    Long proyectoId,
    Long parentId,
    TipoTarea tipo,
    String codigo,
    String nombre,
    String descripcion,
    int orden,
    Long responsableId,
    Integer duracionDias,
    LocalDate inicioPlan,
    LocalDate finPlan,
    LocalDate inicioBase,
    LocalDate finBase,
    LocalDate inicioReal,
    LocalDate finReal,
    BigDecimal avance,
    BigDecimal costoPlan,
    BigDecimal costoReal,
    BigDecimal costoBase) {

    public static TareaResponse de(Tarea t) {
        return new TareaResponse(t.id, t.proyectoId, t.parentId, t.tipo, t.codigo, t.nombre, t.descripcion,
            t.orden, t.responsableId, t.duracionDias, t.inicioPlan, t.finPlan, t.inicioBase, t.finBase,
            t.inicioReal, t.finReal, t.avance, t.costoPlan, t.costoReal, t.costoBase);
    }
}
