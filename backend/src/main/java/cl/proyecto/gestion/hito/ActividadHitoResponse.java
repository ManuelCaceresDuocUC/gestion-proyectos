package cl.proyecto.gestion.hito;

import cl.proyecto.gestion.proyecto.Tarea;
import java.time.LocalDate;

/** Actividad asociada, con el término contra el que se valida la fecha del hito. */
public record ActividadHitoResponse(
    Long id,
    String codigo,
    String nombre,
    LocalDate finPlan,
    LocalDate finReal
) {
    static ActividadHitoResponse de(Tarea t) {
        return new ActividadHitoResponse(t.id, t.codigo, t.nombre, t.finPlan, t.finReal);
    }
}
