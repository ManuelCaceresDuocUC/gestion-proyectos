package cl.proyecto.gestion.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Nodo del árbol de la EDT con sus hijos anidados (GET /api/proyectos/{id}/edt). Lo usan el árbol y el Gantt. */
public record NodoEdt(
    Long id,
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
    BigDecimal avance,
    BigDecimal costoPlan,
    BigDecimal costoReal,
    List<NodoEdt> hijos) {

    public static NodoEdt de(Tarea t, List<NodoEdt> hijos) {
        return new NodoEdt(t.id, t.parentId, t.tipo, t.codigo, t.nombre, t.descripcion, t.orden,
            t.responsableId, t.duracionDias, t.inicioPlan, t.finPlan, t.avance, t.costoPlan, t.costoReal, hijos);
    }
}
