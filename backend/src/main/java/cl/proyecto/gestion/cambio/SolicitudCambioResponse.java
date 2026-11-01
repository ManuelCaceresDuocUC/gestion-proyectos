package cl.proyecto.gestion.cambio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** JSON de salida. diasAtraso se calcula; no es columna de la tabla. */
public record SolicitudCambioResponse(
    Long id,
    Long proyectoId,
    String codigo,
    String nombre,
    String descripcion,
    String solicitante,
    Long responsableId,
    LocalDate fechaSolicitud,
    LocalDate inicioPrevisto,
    LocalDate finPrevisto,
    LocalDate finReal,
    PrioridadSolicitud prioridad,
    TipoSolicitud tipo,
    RiesgoSolicitud riesgo,
    ImpactoSolicitud impacto,
    EstadoSolicitud estado,
    Instant aprobadaEn,
    Long tareaId,
    int impactoDias,
    BigDecimal impactoCosto,
    String impactoAlcance,
    String impactoRecursos,
    ResultadoEvaluacion resultadoEvaluacion,
    boolean aplicada,
    Instant aplicadaEn,
    BigDecimal presupuestoPrevisto,
    BigDecimal costoReal,
    String accionCorrectiva,
    String resultado,
    Instant creadoEn,
    int diasAtraso) {

    public static SolicitudCambioResponse de(SolicitudCambio s) {
        return de(s, LocalDate.now());
    }

    public static SolicitudCambioResponse de(SolicitudCambio s, LocalDate hoy) {
        return new SolicitudCambioResponse(
            s.id, s.proyectoId, s.codigo, s.nombre, s.descripcion, s.solicitante, s.responsableId,
            s.fechaSolicitud, s.inicioPrevisto, s.finPrevisto, s.finReal, s.prioridad, s.tipo,
            s.riesgo, s.impacto, s.estado, s.aprobadaEn, s.tareaId, s.impactoDias, s.impactoCosto,
            s.impactoAlcance, s.impactoRecursos, s.resultadoEvaluacion, s.aplicada, s.aplicadaEn,
            s.presupuestoPrevisto, s.costoReal, s.accionCorrectiva, s.resultado, s.creadoEn,
            diasAtraso(s, hoy));
    }

    /** Si hay fin real: finReal - finPrevisto. Si no y no está cerrada/rechazada: hoy - finPrevisto. Mínimo 0. */
    static int diasAtraso(SolicitudCambio s, LocalDate hoy) {
        if (s.finPrevisto == null) {
            return 0;
        }
        if (s.finReal != null) {
            return (int) Math.max(0, ChronoUnit.DAYS.between(s.finPrevisto, s.finReal));
        }
        if (s.estado == EstadoSolicitud.CERRADO || s.estado == EstadoSolicitud.RECHAZADO) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(s.finPrevisto, hoy));
    }
}
