package cl.proyecto.gestion.cambio;

import java.time.Instant;

public record CambioEtapaResponse(
    Long id,
    Long solicitudId,
    short numero,
    String nombre,
    EstadoEtapa estado,
    Instant actualizadoEn,
    Long actualizadoPor) {

    public static CambioEtapaResponse de(CambioEtapa e) {
        return new CambioEtapaResponse(
            e.id, e.solicitudId, e.numero, e.nombre, e.estado, e.actualizadoEn, e.actualizadoPor);
    }
}
