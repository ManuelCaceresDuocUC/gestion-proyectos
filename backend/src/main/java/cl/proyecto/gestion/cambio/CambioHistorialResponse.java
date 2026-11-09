package cl.proyecto.gestion.cambio;

import java.time.Instant;

public record CambioHistorialResponse(
    Long id,
    Long solicitudId,
    String campo,
    String valorAnterior,
    String valorNuevo,
    Long usuarioId,
    String comentario,
    Instant fecha) {

    public static CambioHistorialResponse de(CambioHistorial h) {
        return new CambioHistorialResponse(
            h.id, h.solicitudId, h.campo, h.valorAnterior, h.valorNuevo, h.usuarioId, h.comentario, h.fecha);
    }
}
