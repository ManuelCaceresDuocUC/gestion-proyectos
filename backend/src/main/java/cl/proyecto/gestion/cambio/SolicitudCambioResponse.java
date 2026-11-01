package cl.proyecto.gestion.cambio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** JSON de salida. diasAtraso y avance de etapas se calculan; no son columnas. */
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
    int diasAtraso,
    int etapasCompletas,
    int avance,
    int etapasBloqueadas) {

    public static SolicitudCambioResponse de(SolicitudCambio s) {
        return de(s, LocalDate.now(), List.of());
    }

    public static SolicitudCambioResponse de(SolicitudCambio s, LocalDate hoy) {
        return de(s, hoy, List.of());
    }

    public static SolicitudCambioResponse de(SolicitudCambio s, List<CambioEtapa> etapas) {
        return de(s, LocalDate.now(), etapas);
    }

    public static SolicitudCambioResponse de(SolicitudCambio s, LocalDate hoy, List<CambioEtapa> etapas) {
        return new SolicitudCambioResponse(
            s.id, s.proyectoId, s.codigo, s.nombre, s.descripcion, s.solicitante, s.responsableId,
            s.fechaSolicitud, s.inicioPrevisto, s.finPrevisto, s.finReal, s.prioridad, s.tipo,
            s.riesgo, s.impacto, s.estado, s.aprobadaEn, s.tareaId, s.impactoDias, s.impactoCosto,
            s.impactoAlcance, s.impactoRecursos, s.resultadoEvaluacion, s.aplicada, s.aplicadaEn,
            s.presupuestoPrevisto, s.costoReal, s.accionCorrectiva, s.resultado, s.creadoEn,
            diasAtraso(s, hoy),
            CambioEtapaService.etapasCompletas(etapas),
            CambioEtapaService.avancePorcentaje(etapas),
            CambioEtapaService.etapasBloqueadas(etapas));
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
