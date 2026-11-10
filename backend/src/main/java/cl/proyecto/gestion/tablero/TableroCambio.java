package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.CambioEtapaService;
import cl.proyecto.gestion.cambio.EstadoEtapa;
import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.ResultadoEvaluacion;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.kpi.KpiEstadoResponse;
import cl.proyecto.gestion.kpi.SemaforoKpi;
import cl.proyecto.gestion.kpi.VariablesCambio;
import cl.proyecto.gestion.tablero.TableroCambioResponse.AlertaTableroCambio;
import cl.proyecto.gestion.tablero.TableroCambioResponse.ConteosTableroCambio;
import cl.proyecto.gestion.tablero.TableroCambioResponse.CostosTableroCambio;
import cl.proyecto.gestion.tablero.TableroCambioResponse.EfectividadTableroCambio;
import cl.proyecto.gestion.tablero.TableroCambioResponse.EtapasTableroCambio;
import cl.proyecto.gestion.tablero.TableroCambioResponse.TiemposTableroCambio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrega el tablero de cambio en memoria. No persiste ni recalcula.
 * Atraso y correctiva siguen las reglas de {@link VariablesCambio}.
 */
public final class TableroCambio {

    public static final String ALERTA_ATRASO = "ATRASO";
    public static final String ALERTA_BLOQUEO = "BLOQUEO";
    public static final String ALERTA_RIESGO_CRITICO = "RIESGO_CRITICO";
    public static final String ALERTA_KPI_ROJO = "KPI_ROJO";

    private TableroCambio() {
    }

    public static TableroCambioResponse resumir(
            Long proyectoId,
            List<SolicitudCambio> solicitudes,
            Map<Long, List<CambioEtapa>> etapasPorSolicitud,
            LocalDate hoy,
            List<KpiEstadoResponse> kpiCambio) {
        if (proyectoId == null) {
            throw new IllegalArgumentException("El proyecto es obligatorio.");
        }
        if (solicitudes == null) {
            throw new IllegalArgumentException("Las solicitudes son obligatorias.");
        }
        if (etapasPorSolicitud == null) {
            throw new IllegalArgumentException("Las etapas son obligatorias.");
        }
        if (hoy == null) {
            throw new IllegalArgumentException("La fecha de referencia es obligatoria.");
        }
        List<KpiEstadoResponse> kpi = kpiCambio == null ? List.of() : List.copyOf(kpiCambio);
        Map<String, BigDecimal> vars = VariablesCambio.calcular(solicitudes, etapasPorSolicitud, hoy);

        EnumMap<EstadoSolicitud, Integer> estados = new EnumMap<>(EstadoSolicitud.class);
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            estados.put(estado, 0);
        }
        int efectivo = 0;
        int parcial = 0;
        int noEfectivo = 0;
        int sinDato = 0;
        int riesgoAlto = 0;
        int riesgoCritico = 0;
        int avanceSuma = 0;
        List<AlertaTableroCambio> alertas = new ArrayList<>();

        for (SolicitudCambio solicitud : solicitudes) {
            estados.merge(solicitud.estado, 1, Integer::sum);
            if (solicitud.riesgo == RiesgoSolicitud.ALTO) {
                riesgoAlto++;
            } else if (solicitud.riesgo == RiesgoSolicitud.CRITICO) {
                riesgoCritico++;
                alertas.add(new AlertaTableroCambio(
                        ALERTA_RIESGO_CRITICO,
                        "La solicitud " + codigoDe(solicitud) + " es de riesgo crítico.",
                        solicitud.codigo));
            }
            if (solicitud.resultadoEvaluacion == ResultadoEvaluacion.EFECTIVO) {
                efectivo++;
            } else if (solicitud.resultadoEvaluacion == ResultadoEvaluacion.PARCIAL) {
                parcial++;
            } else if (solicitud.resultadoEvaluacion == ResultadoEvaluacion.NO_EFECTIVO) {
                noEfectivo++;
            } else {
                sinDato++;
            }
            List<CambioEtapa> etapas = etapasDe(solicitud, etapasPorSolicitud);
            avanceSuma += CambioEtapaService.avancePorcentaje(etapas);
            int atraso = VariablesCambio.diasAtraso(solicitud, hoy);
            if (atraso > 0) {
                alertas.add(new AlertaTableroCambio(
                        ALERTA_ATRASO,
                        "La solicitud " + codigoDe(solicitud) + " tiene " + atraso + " día(s) de atraso.",
                        solicitud.codigo));
            }
            if (etapas.stream().anyMatch(e -> e.estado == EstadoEtapa.BLOQUEADO)) {
                alertas.add(new AlertaTableroCambio(
                        ALERTA_BLOQUEO,
                        "La solicitud " + codigoDe(solicitud) + " tiene etapas bloqueadas.",
                        solicitud.codigo));
            }
        }
        for (KpiEstadoResponse fila : kpi) {
            if (SemaforoKpi.ROJO.name().equals(fila.semaforo())) {
                alertas.add(new AlertaTableroCambio(
                        ALERTA_KPI_ROJO,
                        "El KPI " + fila.codigo() + " está en rojo.",
                        null));
            }
        }

        int total = solicitudes.size();
        Map<String, Integer> porEstado = new LinkedHashMap<>();
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            porEstado.put(estado.name(), estados.get(estado));
        }
        int enCurso = estados.get(EstadoSolicitud.EN_EVALUACION) + estados.get(EstadoSolicitud.EN_IMPLEMENTACION);
        int diasAtrasoTotal = vars.get(VariablesCambio.DIAS_ATRASO_CAMBIOS).intValue();
        BigDecimal promedio = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : vars.get(VariablesCambio.DIAS_ATRASO_CAMBIOS)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        int avancePromedio = total == 0 ? 0 : avanceSuma / total;

        return new TableroCambioResponse(
                proyectoId,
                new ConteosTableroCambio(
                        total,
                        Map.copyOf(porEstado),
                        enCurso,
                        vars.get(VariablesCambio.CAMBIOS_CON_ATRASO).intValue(),
                        riesgoAlto,
                        riesgoCritico,
                        vars.get(VariablesCambio.CAMBIOS_CON_BLOQUEO).intValue(),
                        vars.get(VariablesCambio.CAMBIOS_CON_CORRECTIVA).intValue(),
                        vars.get(VariablesCambio.CAMBIOS_ADOPTADOS).intValue()),
                new EtapasTableroCambio(avancePromedio),
                new CostosTableroCambio(
                        vars.get(VariablesCambio.PRESUPUESTO_CAMBIOS_CERRADOS),
                        vars.get(VariablesCambio.COSTO_REAL_CAMBIOS_CERRADOS)),
                new TiemposTableroCambio(diasAtrasoTotal, promedio),
                new EfectividadTableroCambio(efectivo, parcial, noEfectivo, sinDato),
                List.copyOf(alertas),
                kpi);
    }

    private static List<CambioEtapa> etapasDe(
            SolicitudCambio solicitud, Map<Long, List<CambioEtapa>> etapasPorSolicitud) {
        if (solicitud.id == null) {
            return List.of();
        }
        return etapasPorSolicitud.getOrDefault(solicitud.id, List.of());
    }

    private static String codigoDe(SolicitudCambio solicitud) {
        return solicitud.codigo == null ? "#" + solicitud.id : solicitud.codigo;
    }
}
