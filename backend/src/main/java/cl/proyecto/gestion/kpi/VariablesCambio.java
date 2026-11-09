package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.EstadoEtapa;
import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Variables de las fórmulas KC-01…KC-13. Dominio puro: cuenta solicitudes y etapas
 * ya cargadas. El atraso replica la regla de C-02 (días corridos, mínimo 0).
 */
public final class VariablesCambio {

    public static final String CAMBIOS_TOTAL = "CAMBIOS_TOTAL";
    public static final String CAMBIOS_CERRADOS = "CAMBIOS_CERRADOS";
    public static final String CAMBIOS_CERRADOS_EN_PLAZO = "CAMBIOS_CERRADOS_EN_PLAZO";
    public static final String CAMBIOS_RIESGO_ALTO = "CAMBIOS_RIESGO_ALTO";
    public static final String CAMBIOS_PRIORIDAD_ALTA = "CAMBIOS_PRIORIDAD_ALTA";
    public static final String CAMBIOS_CON_ATRASO = "CAMBIOS_CON_ATRASO";
    public static final String CAMBIOS_CON_BLOQUEO = "CAMBIOS_CON_BLOQUEO";
    public static final String CAMBIOS_CON_CORRECTIVA = "CAMBIOS_CON_CORRECTIVA";
    public static final String CAMBIOS_CERRADOS_CON_SOBRECOSTO = "CAMBIOS_CERRADOS_CON_SOBRECOSTO";
    public static final String CAMBIOS_CERRADOS_CON_PRESUPUESTO = "CAMBIOS_CERRADOS_CON_PRESUPUESTO";
    public static final String ETAPAS_COMPLETADAS = "ETAPAS_COMPLETADAS";
    public static final String ETAPAS_TOTALES = "ETAPAS_TOTALES";
    public static final String CAMBIOS_ADOPTADOS = "CAMBIOS_ADOPTADOS";
    public static final String COSTO_REAL_CAMBIOS_CERRADOS = "COSTO_REAL_CAMBIOS_CERRADOS";
    public static final String PRESUPUESTO_CAMBIOS_CERRADOS = "PRESUPUESTO_CAMBIOS_CERRADOS";
    public static final String DIAS_ATRASO_CAMBIOS = "DIAS_ATRASO_CAMBIOS";
    public static final String CAMBIOS_APROBADOS = "CAMBIOS_APROBADOS";
    public static final String CAMBIOS_APROBADOS_APLICADOS = "CAMBIOS_APROBADOS_APLICADOS";

    /** Diez etapas por solicitud del proyecto. «Seguidos» = todas las del proyecto. */
    public static final int ETAPAS_POR_SOLICITUD = 10;

    private static final short ETAPA_CORRECTIVA = 9;
    private static final short ETAPA_ADOPCION = 10;

    private static final Set<EstadoSolicitud> APROBADOS = Set.of(
            EstadoSolicitud.APROBADO,
            EstadoSolicitud.EN_IMPLEMENTACION,
            EstadoSolicitud.CERRADO);

    private VariablesCambio() {
    }

    public static Set<String> nombres() {
        return Set.copyOf(calcular(List.of(), Map.of(), LocalDate.EPOCH).keySet());
    }

    public static Map<String, BigDecimal> calcular(
            List<SolicitudCambio> solicitudes,
            Map<Long, List<CambioEtapa>> etapasPorSolicitud,
            LocalDate hoy) {
        if (solicitudes == null) {
            throw new IllegalArgumentException("Las solicitudes son obligatorias.");
        }
        if (etapasPorSolicitud == null) {
            throw new IllegalArgumentException("Las etapas son obligatorias.");
        }
        if (hoy == null) {
            throw new IllegalArgumentException("La fecha de referencia es obligatoria.");
        }

        int cerrados = 0;
        int cerradosEnPlazo = 0;
        int riesgoAlto = 0;
        int prioridadAlta = 0;
        int conAtraso = 0;
        int conBloqueo = 0;
        int conCorrectiva = 0;
        int cerradosConSobrecosto = 0;
        int cerradosConPresupuesto = 0;
        int etapasCompletadas = 0;
        int adoptados = 0;
        int diasAtraso = 0;
        int aprobados = 0;
        int aplicados = 0;
        BigDecimal costoRealCerrados = BigDecimal.ZERO;
        BigDecimal presupuestoCerrados = BigDecimal.ZERO;

        for (SolicitudCambio solicitud : solicitudes) {
            int atraso = diasAtraso(solicitud, hoy);
            diasAtraso += atraso;
            List<CambioEtapa> etapas = etapasDe(solicitud, etapasPorSolicitud);
            boolean cerrada = solicitud.estado == EstadoSolicitud.CERRADO;

            if (cerrada) {
                cerrados++;
                if (atraso == 0) {
                    cerradosEnPlazo++;
                }
                if (solicitud.presupuestoPrevisto != null) {
                    cerradosConPresupuesto++;
                    presupuestoCerrados = presupuestoCerrados.add(solicitud.presupuestoPrevisto);
                    if (solicitud.costoReal != null
                            && solicitud.costoReal.compareTo(solicitud.presupuestoPrevisto) > 0) {
                        cerradosConSobrecosto++;
                    }
                }
                if (solicitud.costoReal != null) {
                    costoRealCerrados = costoRealCerrados.add(solicitud.costoReal);
                }
            }
            if (solicitud.riesgo == RiesgoSolicitud.ALTO || solicitud.riesgo == RiesgoSolicitud.CRITICO) {
                riesgoAlto++;
            }
            if (solicitud.prioridad == PrioridadSolicitud.ALTA
                    || solicitud.prioridad == PrioridadSolicitud.CRITICA) {
                prioridadAlta++;
            }
            if (atraso > 0) {
                conAtraso++;
            }
            if (etapas.stream().anyMatch(e -> e.estado == EstadoEtapa.BLOQUEADO)) {
                conBloqueo++;
            }
            if (tieneCorrectiva(solicitud, etapas)) {
                conCorrectiva++;
            }
            etapasCompletadas += (int) etapas.stream()
                    .filter(e -> e.estado == EstadoEtapa.COMPLETADO)
                    .count();
            if (etapas.stream().anyMatch(e -> e.numero == ETAPA_ADOPCION && e.estado == EstadoEtapa.COMPLETADO)) {
                adoptados++;
            }
            if (APROBADOS.contains(solicitud.estado)) {
                aprobados++;
            }
            if (solicitud.aplicada) {
                aplicados++;
            }
        }

        Map<String, BigDecimal> variables = new LinkedHashMap<>();
        variables.put(CAMBIOS_TOTAL, entero(solicitudes.size()));
        variables.put(CAMBIOS_CERRADOS, entero(cerrados));
        variables.put(CAMBIOS_CERRADOS_EN_PLAZO, entero(cerradosEnPlazo));
        variables.put(CAMBIOS_RIESGO_ALTO, entero(riesgoAlto));
        variables.put(CAMBIOS_PRIORIDAD_ALTA, entero(prioridadAlta));
        variables.put(CAMBIOS_CON_ATRASO, entero(conAtraso));
        variables.put(CAMBIOS_CON_BLOQUEO, entero(conBloqueo));
        variables.put(CAMBIOS_CON_CORRECTIVA, entero(conCorrectiva));
        variables.put(CAMBIOS_CERRADOS_CON_SOBRECOSTO, entero(cerradosConSobrecosto));
        variables.put(CAMBIOS_CERRADOS_CON_PRESUPUESTO, entero(cerradosConPresupuesto));
        variables.put(ETAPAS_COMPLETADAS, entero(etapasCompletadas));
        variables.put(ETAPAS_TOTALES, entero(solicitudes.size() * ETAPAS_POR_SOLICITUD));
        variables.put(CAMBIOS_ADOPTADOS, entero(adoptados));
        variables.put(COSTO_REAL_CAMBIOS_CERRADOS, costoRealCerrados);
        variables.put(PRESUPUESTO_CAMBIOS_CERRADOS, presupuestoCerrados);
        variables.put(DIAS_ATRASO_CAMBIOS, entero(diasAtraso));
        variables.put(CAMBIOS_APROBADOS, entero(aprobados));
        variables.put(CAMBIOS_APROBADOS_APLICADOS, entero(aplicados));
        return Map.copyOf(variables);
    }

    /**
     * Misma regla que {@code SolicitudCambioResponse.diasAtraso}: con fin real,
     * fin real − fin previsto; sin fin real y estado cerrado o rechazado, 0;
     * sin fin real en otro estado, hoy − fin previsto. Sin fin previsto, 0. Mínimo 0.
     */
    static int diasAtraso(SolicitudCambio solicitud, LocalDate hoy) {
        if (solicitud.finPrevisto == null) {
            return 0;
        }
        if (solicitud.finReal != null) {
            return (int) Math.max(0, ChronoUnit.DAYS.between(solicitud.finPrevisto, solicitud.finReal));
        }
        if (solicitud.estado == EstadoSolicitud.CERRADO || solicitud.estado == EstadoSolicitud.RECHAZADO) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(solicitud.finPrevisto, hoy));
    }

    private static List<CambioEtapa> etapasDe(
            SolicitudCambio solicitud, Map<Long, List<CambioEtapa>> etapasPorSolicitud) {
        if (solicitud.id == null) {
            return List.of();
        }
        return etapasPorSolicitud.getOrDefault(solicitud.id, List.of());
    }

    private static boolean tieneCorrectiva(SolicitudCambio solicitud, List<CambioEtapa> etapas) {
        if (solicitud.accionCorrectiva != null && !solicitud.accionCorrectiva.isBlank()) {
            return true;
        }
        return etapas.stream().anyMatch(e -> e.numero == ETAPA_CORRECTIVA
                && e.estado != EstadoEtapa.PENDIENTE
                && e.estado != EstadoEtapa.NO_APLICA);
    }

    private static BigDecimal entero(int valor) {
        return BigDecimal.valueOf(valor);
    }
}
