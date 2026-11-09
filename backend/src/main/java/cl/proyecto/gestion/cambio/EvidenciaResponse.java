package cl.proyecto.gestion.cambio;

import java.time.Instant;

public record EvidenciaResponse(
    Long id,
    Long solicitudId,
    String descripcion,
    String url,
    Long creadoPor,
    Instant creadoEn) {

    public static EvidenciaResponse de(Evidencia e) {
        return new EvidenciaResponse(e.id, e.solicitudId, e.descripcion, e.url, e.creadoPor, e.creadoEn);
    }
}
