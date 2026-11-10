package cl.proyecto.gestion.tablero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.EstadoEtapa;
import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.ResultadoEvaluacion;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.kpi.KpiEstadoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Agregados del tablero de cambio. Sin Spring. */
class TableroCambioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    @Test
    void sinSolicitudesTodoQuedaEnCero() {
        TableroCambioResponse r = TableroCambio.resumir(7L, List.of(), Map.of(), HOY, List.of());

        assertThat(r.proyectoId()).isEqualTo(7L);
        assertThat(r.conteos().total()).isZero();
        assertThat(r.conteos().porEstado())
                .containsEntry("ABIERTO", 0)
                .containsEntry("EN_EVALUACION", 0)
                .containsEntry("APROBADO", 0)
                .containsEntry("RECHAZADO", 0)
                .containsEntry("EN_IMPLEMENTACION", 0)
                .containsEntry("CERRADO", 0);
        assertThat(r.conteos().enCurso()).isZero();
        assertThat(r.etapas().avancePromedio()).isZero();
        assertThat(r.tiempos().diasAtrasoPromedio()).isEqualByComparingTo("0.00");
        assertThat(r.efectividad().sinDato()).isZero();
        assertThat(r.alertas()).isEmpty();
        assertThat(r.kpi()).isEmpty();
    }

    @Test
    void cuentaEstadosRiesgosEtapasYAlertas() {
        SolicitudCambio cerrada = solicitud(1L, "SC-01", EstadoSolicitud.CERRADO);
        cerrada.finPrevisto = LocalDate.of(2026, 11, 1);
        cerrada.finReal = LocalDate.of(2026, 11, 1);
        cerrada.riesgo = RiesgoSolicitud.ALTO;
        cerrada.resultadoEvaluacion = ResultadoEvaluacion.EFECTIVO;
        cerrada.presupuestoPrevisto = new BigDecimal("100.00");
        cerrada.costoReal = new BigDecimal("80.00");

        SolicitudCambio enCurso = solicitud(2L, "SC-02", EstadoSolicitud.EN_IMPLEMENTACION);
        enCurso.finPrevisto = LocalDate.of(2026, 11, 1);
        enCurso.riesgo = RiesgoSolicitud.CRITICO;
        enCurso.accionCorrectiva = "revisar";

        SolicitudCambio evaluacion = solicitud(3L, "SC-03", EstadoSolicitud.EN_EVALUACION);
        evaluacion.resultadoEvaluacion = ResultadoEvaluacion.PARCIAL;

        Map<Long, List<CambioEtapa>> etapas = Map.of(
                1L, List.of(etapa(1L, 10, EstadoEtapa.COMPLETADO)),
                2L, List.of(etapa(2L, 3, EstadoEtapa.BLOQUEADO)));

        KpiEstadoResponse rojo = new KpiEstadoResponse(
                9L, "KC-01", "cerrados", "CAMBIO", "x", "%", "MAYOR_MEJOR", "MAYOR_IGUAL",
                new BigDecimal("80"), BigDecimal.ZERO, BigDecimal.ZERO, "ROJO",
                "RECALCULO", null, false, true, "ROJO");

        TableroCambioResponse r = TableroCambio.resumir(
                4L, List.of(cerrada, enCurso, evaluacion), etapas, HOY, List.of(rojo));

        assertThat(r.conteos().total()).isEqualTo(3);
        assertThat(r.conteos().porEstado()).containsEntry("CERRADO", 1).containsEntry("EN_IMPLEMENTACION", 1)
                .containsEntry("EN_EVALUACION", 1);
        assertThat(r.conteos().enCurso()).isEqualTo(2);
        assertThat(r.conteos().atrasados()).isEqualTo(1);
        assertThat(r.conteos().riesgoAlto()).isEqualTo(1);
        assertThat(r.conteos().riesgoCritico()).isEqualTo(1);
        assertThat(r.conteos().conBloqueo()).isEqualTo(1);
        assertThat(r.conteos().conCorrectiva()).isEqualTo(1);
        assertThat(r.conteos().adoptados()).isEqualTo(1);
        assertThat(r.etapas().avancePromedio()).isEqualTo(3);
        assertThat(r.costos().presupuestoCerradas()).isEqualByComparingTo("100.00");
        assertThat(r.costos().costoRealCerradas()).isEqualByComparingTo("80.00");
        assertThat(r.tiempos().diasAtrasoTotal()).isEqualTo(11);
        assertThat(r.tiempos().diasAtrasoPromedio()).isEqualByComparingTo("3.67");
        assertThat(r.efectividad().efectivo()).isEqualTo(1);
        assertThat(r.efectividad().parcial()).isEqualTo(1);
        assertThat(r.efectividad().sinDato()).isEqualTo(1);
        assertThat(r.alertas()).extracting(TableroCambioResponse.AlertaTableroCambio::tipo)
                .containsExactly(
                        TableroCambio.ALERTA_RIESGO_CRITICO,
                        TableroCambio.ALERTA_ATRASO,
                        TableroCambio.ALERTA_BLOQUEO,
                        TableroCambio.ALERTA_KPI_ROJO);
        assertThat(r.kpi()).containsExactly(rojo);
    }

    @Test
    void argumentosNulosSeRechazan() {
        assertThatThrownBy(() -> TableroCambio.resumir(null, List.of(), Map.of(), HOY, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TableroCambio.resumir(1L, null, Map.of(), HOY, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TableroCambio.resumir(1L, List.of(), null, HOY, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TableroCambio.resumir(1L, List.of(), Map.of(), null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SolicitudCambio solicitud(Long id, String codigo, EstadoSolicitud estado) {
        SolicitudCambio s = new SolicitudCambio();
        s.id = id;
        s.codigo = codigo;
        s.estado = estado;
        s.riesgo = RiesgoSolicitud.MEDIO;
        return s;
    }

    private static CambioEtapa etapa(Long solicitudId, int numero, EstadoEtapa estado) {
        CambioEtapa e = new CambioEtapa();
        e.solicitudId = solicitudId;
        e.numero = (short) numero;
        e.estado = estado;
        return e;
    }
}
