package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;
import java.time.Instant;

public record KpiEstadoResponse(
        Long id,
        String codigo,
        String nombre,
        String categoria,
        String formula,
        String unidad,
        String sentido,
        String operadorMeta,
        BigDecimal meta,
        BigDecimal valor,
        BigDecimal cumplimiento,
        String semaforo,
        String motivo,
        Instant calculadoEn,
        boolean noCalculable,
        boolean alerta,
        String alertaMotivo) {
}
