package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.kpi.KpiEstadoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** JSON de {@code GET /api/tablero/cambio/{proyectoId}} (C-12). Solo lectura. */
public record TableroCambioResponse(
        Long proyectoId,
        ConteosTableroCambio conteos,
        EtapasTableroCambio etapas,
        CostosTableroCambio costos,
        TiemposTableroCambio tiempos,
        EfectividadTableroCambio efectividad,
        List<AlertaTableroCambio> alertas,
        List<KpiEstadoResponse> kpi) {

    public record ConteosTableroCambio(
            int total,
            Map<String, Integer> porEstado,
            int enCurso,
            int atrasados,
            int riesgoAlto,
            int riesgoCritico,
            int conBloqueo,
            int conCorrectiva,
            int adoptados) {
    }

    public record EtapasTableroCambio(int avancePromedio) {
    }

    public record CostosTableroCambio(BigDecimal presupuestoCerradas, BigDecimal costoRealCerradas) {
    }

    public record TiemposTableroCambio(int diasAtrasoTotal, BigDecimal diasAtrasoPromedio) {
    }

    public record EfectividadTableroCambio(int efectivo, int parcial, int noEfectivo, int sinDato) {
    }

    public record AlertaTableroCambio(String tipo, String mensaje, String solicitudCodigo) {
    }
}
