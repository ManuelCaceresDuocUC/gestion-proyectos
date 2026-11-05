package cl.proyecto.gestion.cambio;

import cl.proyecto.gestion.common.ApiException;
import java.util.Map;
import java.util.Set;

/**
 * Grafo de estados de una solicitud (C-08). Sin Spring: el servicio lo consulta al PUT,
 * aprobar, rechazar y aplicar. El mismo estado se considera válido (el formulario reenvía el actual).
 *
 * <pre>
 * ABIERTO            → EN_EVALUACION
 * EN_EVALUACION      → ABIERTO | APROBADO | RECHAZADO
 * APROBADO           → EN_IMPLEMENTACION
 * EN_IMPLEMENTACION  → CERRADO
 * RECHAZADO, CERRADO → (terminal)
 * </pre>
 */
public final class TransicionesSolicitud {

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> PERMITIDAS = Map.of(
        EstadoSolicitud.ABIERTO, Set.of(EstadoSolicitud.EN_EVALUACION),
        EstadoSolicitud.EN_EVALUACION, Set.of(
            EstadoSolicitud.ABIERTO, EstadoSolicitud.APROBADO, EstadoSolicitud.RECHAZADO),
        EstadoSolicitud.APROBADO, Set.of(EstadoSolicitud.EN_IMPLEMENTACION),
        EstadoSolicitud.EN_IMPLEMENTACION, Set.of(EstadoSolicitud.CERRADO),
        EstadoSolicitud.RECHAZADO, Set.of(),
        EstadoSolicitud.CERRADO, Set.of()
    );

    private TransicionesSolicitud() {}

    public static boolean permite(EstadoSolicitud desde, EstadoSolicitud hacia) {
        if (desde == null || hacia == null) {
            return false;
        }
        if (desde == hacia) {
            return true;
        }
        return PERMITIDAS.getOrDefault(desde, Set.of()).contains(hacia);
    }

    /** 409 {@code TRANSICION_INVALIDA} si el salto no está en el grafo. */
    public static void exigir(EstadoSolicitud desde, EstadoSolicitud hacia) {
        if (!permite(desde, hacia)) {
            throw ApiException.conflicto("TRANSICION_INVALIDA",
                "No se puede pasar de " + etiqueta(desde) + " a " + etiqueta(hacia) + ".");
        }
    }

    public static boolean esTerminal(EstadoSolicitud estado) {
        return estado == EstadoSolicitud.RECHAZADO || estado == EstadoSolicitud.CERRADO;
    }

    public static boolean puedeAprobar(EstadoSolicitud desde) {
        return desde == EstadoSolicitud.EN_EVALUACION;
    }

    public static boolean puedeRechazar(EstadoSolicitud desde) {
        return desde == EstadoSolicitud.EN_EVALUACION;
    }

    /** Aplicar al plan solo desde {@code APROBADO}. La bandera {@code aplicada} la mira el servicio. */
    public static boolean puedeAplicar(EstadoSolicitud desde) {
        return desde == EstadoSolicitud.APROBADO;
    }

    private static String etiqueta(EstadoSolicitud estado) {
        return estado == null ? "(vacío)" : estado.name();
    }
}
