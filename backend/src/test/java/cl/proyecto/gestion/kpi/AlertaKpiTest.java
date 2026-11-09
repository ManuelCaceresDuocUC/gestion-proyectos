package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AlertaKpiTest {

    @Test
    void rojoOMenorCumplimientoQueElSnapshotAnterior() {
        assertThat(AlertaKpi.motivo(SemaforoKpi.VERDE, new BigDecimal("1.0000"), null)).isNull();
        assertThat(AlertaKpi.motivo(SemaforoKpi.ROJO, new BigDecimal("0.5000"), null)).isEqualTo(AlertaKpi.ROJO);
        assertThat(AlertaKpi.motivo(SemaforoKpi.AMARILLO, new BigDecimal("0.9000"), new BigDecimal("1.0000")))
                .isEqualTo(AlertaKpi.EMPEORA);
        assertThat(AlertaKpi.motivo(SemaforoKpi.AMARILLO, new BigDecimal("0.9000"), new BigDecimal("0.9000")))
                .isNull();
        assertThat(AlertaKpi.motivo(SemaforoKpi.ROJO, new BigDecimal("0.4000"), new BigDecimal("0.9000")))
                .isEqualTo(AlertaKpi.ROJO);
        assertThat(AlertaKpi.motivo(null, null, new BigDecimal("1.0000"))).isNull();
    }
}
