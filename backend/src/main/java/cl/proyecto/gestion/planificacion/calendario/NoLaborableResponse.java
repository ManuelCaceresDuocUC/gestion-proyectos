package cl.proyecto.gestion.planificacion.calendario;

import java.time.LocalDate;

public record NoLaborableResponse(
    Long id,
    Long proyectoId,
    LocalDate fecha,
    String descripcion) {

    public static NoLaborableResponse de(NoLaborable noLaborable) {
        return new NoLaborableResponse(
            noLaborable.id,
            noLaborable.proyectoId,
            noLaborable.fecha,
            noLaborable.descripcion);
    }
}
