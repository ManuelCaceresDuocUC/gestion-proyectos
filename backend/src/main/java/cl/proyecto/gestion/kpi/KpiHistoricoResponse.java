package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;
import java.time.Instant;

public record KpiHistoricoResponse(
        Long id,
        Long kpiId,
        Long proyectoId,
        BigDecimal valor,
        BigDecimal cumplimiento,
        String semaforo,
        String motivo,
        Instant calculadoEn,
        boolean noCalculable) {
}
