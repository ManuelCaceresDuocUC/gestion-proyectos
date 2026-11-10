package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.kpi.KpiEstadoResponse;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** JSON de GET /api/tablero/proyecto/{proyectoId}. Nombres fijados en docs/api/tableros.md. */
public record TableroProyectoResponse(
        Long proyectoId,
        String codigo,
        String nombre,
        Avance avance,
        Hitos hitos,
        Costos costos,
        Recursos recursos,
        List<ActividadAtrasada> actividadesAtrasadas,
        List<KpiEstadoResponse> kpi) {

    public record Avance(BigDecimal real, BigDecimal planificado) {
    }

    public record Hitos(List<HitoResumen> cumplidos, List<HitoResumen> proximos, List<HitoResumen> vencidos) {
    }

    public record HitoResumen(
            Long id,
            String codigo,
            String logro,
            LocalDate fechaPlan,
            LocalDate fechaReal,
            String estado) {
    }

    public record Costos(BigDecimal plan, BigDecimal real, BigDecimal presupuesto, String moneda) {
    }

    /** Conteos de personas de la medición de KPI. Las alertas son filas por fecha. */
    public record Recursos(int profesionales, int sobreasignados, List<SobrecargaAlertaResponse> alertas) {
    }

    public record ActividadAtrasada(Long id, String codigo, String nombre, LocalDate finPlan, BigDecimal avance) {
    }
}
