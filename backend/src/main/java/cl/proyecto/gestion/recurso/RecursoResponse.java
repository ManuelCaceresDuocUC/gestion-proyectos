package cl.proyecto.gestion.recurso;

import java.math.BigDecimal;

public record RecursoResponse(
    Long id,
    Long tipoRecursoId,
    String nombre,
    BigDecimal tarifa,
    BigDecimal capacidadHorasDia,
    boolean activo) {

    public static RecursoResponse de(Recurso recurso) {
        return new RecursoResponse(
            recurso.id,
            recurso.tipoRecursoId,
            recurso.nombre,
            recurso.tarifa,
            recurso.capacidadHorasDia,
            recurso.activo);
    }
}