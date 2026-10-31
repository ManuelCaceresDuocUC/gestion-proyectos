package cl.proyecto.gestion.planificacion.lineabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LineaBaseResponse(
    Long id, Long proyectoId, int version, String motivo, Instant creadaEn, LocalDate fechaFin, BigDecimal costoTotal) {

    public static LineaBaseResponse de(LineaBase l) {
        return new LineaBaseResponse(l.id, l.proyectoId, l.version, l.motivo, l.creadaEn, l.fechaFin, l.costoTotal);
    }
}
