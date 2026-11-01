package cl.proyecto.gestion.cambio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos que envía el cliente al crear o editar (POST/PUT).
 * El código y el proyecto salen del servicio / de la URL. aplicada no se manda: al crear queda false.
 */
public record SolicitudCambioRequest(
    @NotBlank @Size(max = 200) String nombre,
    String descripcion,
    @Size(max = 120) String solicitante,
    Long responsableId,
    LocalDate fechaSolicitud,
    LocalDate inicioPrevisto,
    LocalDate finPrevisto,
    LocalDate finReal,
    PrioridadSolicitud prioridad,
    @NotNull TipoSolicitud tipo,
    RiesgoSolicitud riesgo,
    ImpactoSolicitud impacto,
    EstadoSolicitud estado,
    Long tareaId,
    Integer impactoDias,
    BigDecimal impactoCosto,
    String impactoAlcance,
    String impactoRecursos,
    ResultadoEvaluacion resultadoEvaluacion,
    BigDecimal presupuestoPrevisto,
    BigDecimal costoReal,
    String accionCorrectiva,
    String resultado) {}
