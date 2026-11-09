package cl.proyecto.gestion.seed;

import cl.proyecto.gestion.cambio.ImpactoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.TipoSolicitud;
import java.text.Normalizer;
import java.util.Locale;

/** Traduce textos viejos del JSON (Tecnología, Personas, Crítica…) a los enums V21. */
final class SeedMapeo {

    private SeedMapeo() {}

    static TipoSolicitud tipoDe(String raw) {
        String n = norma(raw);
        return switch (n) {
            case "PERSONAS", "PROCESO" -> TipoSolicitud.ORGANIZACION;
            default -> TipoSolicitud.valueOf(n);
        };
    }

    static PrioridadSolicitud prioridadDe(String raw) {
        String n = norma(raw);
        return n.isEmpty() ? null : PrioridadSolicitud.valueOf(n);
    }

    static RiesgoSolicitud riesgoDe(String raw) {
        String n = norma(raw);
        return n.isEmpty() ? null : RiesgoSolicitud.valueOf(n);
    }

    static ImpactoSolicitud impactoDe(String raw) {
        String n = norma(raw);
        return n.isEmpty() ? null : ImpactoSolicitud.valueOf(n);
    }

    static String norma(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String n = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toUpperCase(Locale.ROOT);
    }
}
