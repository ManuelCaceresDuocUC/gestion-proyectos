package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.EstadoEtapa;
import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Conteos KC sobre las mismas reglas que los registros de cambio. Sin Spring. */
class VariablesCambioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    @Test
    void sinSolicitudesTodoQuedaEnCero() {
        Map<String, BigDecimal> variables = VariablesCambio.calcular(List.of(), Map.of(), HOY);

        assertThat(variables).hasSize(18);
        assertThat(variables.values()).allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);
        assertThat(VariablesCambio.nombres()).containsExactlyInAnyOrderElementsOf(variables.keySet());
    }

    @Test
    void cuentaCierrePlazoRiesgoPrioridadYAtrasoComoLaPlantilla() {
        SolicitudCambio cerradaEnPlazo = solicitud(1L, EstadoSolicitud.CERRADO);
        cerradaEnPlazo.finPrevisto = LocalDate.of(2026, 11, 1);
        cerradaEnPlazo.finReal = LocalDate.of(2026, 11, 1);
        cerradaEnPlazo.riesgo = RiesgoSolicitud.ALTO;
        cerradaEnPlazo.prioridad = PrioridadSolicitud.MEDIA;
        cerradaEnPlazo.presupuestoPrevisto = new BigDecimal("100.00");
        cerradaEnPlazo.costoReal = new BigDecimal("120.00");
        cerradaEnPlazo.aplicada = true;

        SolicitudCambio enCurso = solicitud(2L, EstadoSolicitud.EN_IMPLEMENTACION);
        enCurso.finPrevisto = LocalDate.of(2026, 11, 1);
        enCurso.prioridad = PrioridadSolicitud.CRITICA;
        enCurso.accionCorrectiva = " revisar ";

        SolicitudCambio rechazada = solicitud(3L, EstadoSolicitud.RECHAZADO);
        rechazada.finPrevisto = LocalDate.of(2026, 11, 1);
        rechazada.riesgo = RiesgoSolicitud.CRITICO;

        Map<Long, List<CambioEtapa>> etapas = Map.of(
                1L, List.of(
                        etapa(1L, 9, EstadoEtapa.COMPLETADO),
                        etapa(1L, 10, EstadoEtapa.COMPLETADO)),
                2L, List.of(
                        etapa(2L, 3, EstadoEtapa.BLOQUEADO),
                        etapa(2L, 9, EstadoEtapa.PENDIENTE)));

        Map<String, BigDecimal> v = VariablesCambio.calcular(
                List.of(cerradaEnPlazo, enCurso, rechazada), etapas, HOY);

        assertThat(v.get(VariablesCambio.CAMBIOS_TOTAL)).isEqualByComparingTo("3");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_EN_PLAZO)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_RIESGO_ALTO)).isEqualByComparingTo("2");
        assertThat(v.get(VariablesCambio.CAMBIOS_PRIORIDAD_ALTA)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CON_ATRASO)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.DIAS_ATRASO_CAMBIOS)).isEqualByComparingTo("11");
        assertThat(v.get(VariablesCambio.CAMBIOS_CON_BLOQUEO)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CON_CORRECTIVA)).isEqualByComparingTo("2");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_CON_SOBRECOSTO)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_CON_PRESUPUESTO)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.ETAPAS_COMPLETADAS)).isEqualByComparingTo("2");
        assertThat(v.get(VariablesCambio.ETAPAS_TOTALES)).isEqualByComparingTo("30");
        assertThat(v.get(VariablesCambio.CAMBIOS_ADOPTADOS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.COSTO_REAL_CAMBIOS_CERRADOS)).isEqualByComparingTo("120.00");
        assertThat(v.get(VariablesCambio.PRESUPUESTO_CAMBIOS_CERRADOS)).isEqualByComparingTo("100.00");
        assertThat(v.get(VariablesCambio.CAMBIOS_APROBADOS)).isEqualByComparingTo("2");
        assertThat(v.get(VariablesCambio.CAMBIOS_APROBADOS_APLICADOS)).isEqualByComparingTo("1");
    }

    @Test
    void rechazoSinFinRealNoSumaAtrasoYNoCuentaComoCerradoNiAprobado() {
        SolicitudCambio rechazada = solicitud(4L, EstadoSolicitud.RECHAZADO);
        rechazada.finPrevisto = LocalDate.of(2026, 11, 1);

        Map<String, BigDecimal> v = VariablesCambio.calcular(List.of(rechazada), Map.of(), HOY);

        assertThat(v.get(VariablesCambio.DIAS_ATRASO_CAMBIOS)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.CAMBIOS_APROBADOS)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.CAMBIOS_CON_ATRASO)).isEqualByComparingTo("0");
    }

    @Test
    void etapaNueveNoAplicaSinTextoNoEsCorrectivaYElCostoIgualNoEsSobrecosto() {
        SolicitudCambio cerrada = solicitud(5L, EstadoSolicitud.CERRADO);
        cerrada.presupuestoPrevisto = new BigDecimal("50");
        cerrada.costoReal = new BigDecimal("50");
        cerrada.accionCorrectiva = "  ";

        Map<String, BigDecimal> v = VariablesCambio.calcular(
                List.of(cerrada),
                Map.of(5L, List.of(etapa(5L, 9, EstadoEtapa.NO_APLICA))),
                HOY);

        assertThat(v.get(VariablesCambio.CAMBIOS_CON_CORRECTIVA)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_CON_SOBRECOSTO)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_CON_PRESUPUESTO)).isEqualByComparingTo("1");
    }

    @Test
    void argumentosNulosSeRechazan() {
        assertThatThrownBy(() -> VariablesCambio.calcular(null, Map.of(), HOY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VariablesCambio.calcular(List.of(), null, HOY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VariablesCambio.calcular(List.of(), Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SolicitudCambio solicitud(Long id, EstadoSolicitud estado) {
        SolicitudCambio s = new SolicitudCambio();
        s.id = id;
        s.estado = estado;
        s.riesgo = RiesgoSolicitud.MEDIO;
        s.prioridad = PrioridadSolicitud.MEDIA;
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
