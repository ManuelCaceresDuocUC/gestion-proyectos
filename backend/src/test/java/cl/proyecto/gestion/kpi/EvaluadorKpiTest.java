package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Fórmulas de Actividad_30_KPI y del CPI (VALOR_GANADO / COSTO_REAL). Sin Spring. */
class EvaluadorKpiTest {

    private final EvaluadorKpi evaluador = new EvaluadorKpi();

    @Test
    void cpiConCostoRealPositivoQuedaVerdeYEnLaMeta() {
        var resultado = evaluador.evaluar(
                def("VALOR_GANADO / COSTO_REAL", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1"),
                Map.of("VALOR_GANADO", decimal("120"), "COSTO_REAL", decimal("100")));

        assertThat(resultado.valor()).isEqualByComparingTo("1.20");
        assertThat(resultado.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(resultado.semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(resultado.cumpleMeta()).isTrue();
        assertThat(resultado.noCalculable()).isFalse();
    }

    @Test
    void denominadorCeroEsNoCalculableSinErrorNiInfinito() {
        var resultado = evaluador.evaluar(
                def("VALOR_GANADO / COSTO_REAL", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1"),
                Map.of("VALOR_GANADO", decimal("120"), "COSTO_REAL", BigDecimal.ZERO));

        assertNoCalculable(resultado);
    }

    @Test
    void ceroSobreCeroTambienEsNoCalculable() {
        var resultado = evaluador.evaluar(
                def("VALOR_GANADO / COSTO_REAL", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1"),
                Map.of("VALOR_GANADO", BigDecimal.ZERO, "COSTO_REAL", BigDecimal.ZERO));

        assertNoCalculable(resultado);
    }

    @Test
    void velocidadRedondeaSoloElResultadoFinal() {
        var velocidad = evaluador.evaluar(
                def("ENTREGAS / HORAS", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "30"),
                Map.of("ENTREGAS", decimal("100"), "HORAS", decimal("3")));

        assertThat(velocidad.valor()).isEqualByComparingTo("33.33");
        assertThat(velocidad.semaforo()).isEqualTo(SemaforoKpi.VERDE);

        var idaYVuelta = evaluador.evaluar(
                def("(ENTREGAS / HORAS) * HORAS", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "100"),
                Map.of("ENTREGAS", decimal("100"), "HORAS", decimal("3")));
        assertThat(idaYVuelta.valor()).isEqualByComparingTo("100.00");
    }

    @Test
    void puntualidadEnAmarilloYTotalCeroNoCalculable() {
        var puntualidad = evaluador.evaluar(
                def("A_TIEMPO / TOTAL * 100", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "90"),
                Map.of("A_TIEMPO", decimal("85"), "TOTAL", decimal("100")));

        assertThat(puntualidad.valor()).isEqualByComparingTo("85.00");
        assertThat(puntualidad.cumplimiento()).isEqualByComparingTo("0.9444");
        assertThat(puntualidad.semaforo()).isEqualTo(SemaforoKpi.AMARILLO);
        assertThat(puntualidad.cumpleMeta()).isFalse();

        var sinPedidos = evaluador.evaluar(
                def("A_TIEMPO / TOTAL * 100", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "90"),
                Map.of("A_TIEMPO", decimal("85"), "TOTAL", BigDecimal.ZERO));
        assertNoCalculable(sinPedidos);
    }

    @Test
    void reclamosPorMilPedidos() {
        var pocos = evaluador.evaluar(
                def("RECLAMOS / PEDIDOS * 1000", SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "5"),
                Map.of("RECLAMOS", decimal("3"), "PEDIDOS", decimal("1000")));
        assertThat(pocos.valor()).isEqualByComparingTo("3.00");
        assertThat(pocos.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(pocos.semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(pocos.cumpleMeta()).isTrue();

        var ninguno = evaluador.evaluar(
                def("RECLAMOS / PEDIDOS * 1000", SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "5"),
                Map.of("RECLAMOS", BigDecimal.ZERO, "PEDIDOS", decimal("500")));
        assertThat(ninguno.valor()).isEqualByComparingTo("0.00");
        assertThat(ninguno.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(ninguno.semaforo()).isEqualTo(SemaforoKpi.VERDE);

        var sinPedidos = evaluador.evaluar(
                def("RECLAMOS / PEDIDOS * 1000", SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "5"),
                Map.of("RECLAMOS", decimal("3"), "PEDIDOS", BigDecimal.ZERO));
        assertNoCalculable(sinPedidos);
    }

    @Test
    void operadoresEnElBordeDeLaMeta() {
        assertThat(evaluarValor("10", OperadorMeta.MAYOR_IGUAL, "10", null).cumpleMeta()).isTrue();
        assertThat(evaluarValor("10", OperadorMeta.MENOR_IGUAL, "10", null).cumpleMeta()).isTrue();
        assertThat(evaluarValor("10", OperadorMeta.MAYOR, "10", null).cumpleMeta()).isFalse();
        assertThat(evaluarValor("10", OperadorMeta.MENOR, "10", null).cumpleMeta()).isFalse();
        assertThat(evaluarValor("10.01", OperadorMeta.MAYOR, "10", null).cumpleMeta()).isTrue();
        assertThat(evaluarValor("9.99", OperadorMeta.MENOR, "10", null).cumpleMeta()).isTrue();
    }

    @Test
    void entreIncluyeLosLimites() {
        assertThat(evaluarValor("8", OperadorMeta.ENTRE, "8", "12").cumpleMeta()).isTrue();
        assertThat(evaluarValor("12", OperadorMeta.ENTRE, "8", "12").cumpleMeta()).isTrue();
        assertThat(evaluarValor("7.99", OperadorMeta.ENTRE, "8", "12").cumpleMeta()).isFalse();
        assertThat(evaluarValor("12.01", OperadorMeta.ENTRE, "8", "12").cumpleMeta()).isFalse();
    }

    @Test
    void umbralesEditablesCambianElSemaforo() {
        assertThat(conUmbral("95").semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(conUmbral("85").semaforo()).isEqualTo(SemaforoKpi.AMARILLO);
        assertThat(conUmbral("60").semaforo()).isEqualTo(SemaforoKpi.ROJO);
    }

    @Test
    void semaforoPorDefectoYNoRedondeaElUmbral() {
        assertThat(porcentaje("100").semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(porcentaje("80").semaforo()).isEqualTo(SemaforoKpi.AMARILLO);
        assertThat(porcentaje("79").semaforo()).isEqualTo(SemaforoKpi.ROJO);

        var casiAmarillo = evaluador.evaluar(
                def("VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "100"),
                Map.of("VALOR", decimal("79.96")));
        assertThat(casiAmarillo.cumplimiento()).isEqualByComparingTo("0.7996");
        assertThat(casiAmarillo.semaforo()).isEqualTo(SemaforoKpi.ROJO);
    }

    @Test
    void elCumplimientoNoSuperaUno() {
        var resultado = evaluador.evaluar(
                def("VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "100"),
                Map.of("VALOR", decimal("150")));

        assertThat(resultado.valor()).isEqualByComparingTo("150.00");
        assertThat(resultado.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(resultado.semaforo()).isEqualTo(SemaforoKpi.VERDE);
    }

    @Test
    void menorMejorConValorCeroCumpleAlCienPorCientoAunqueLaMetaSeaCero() {
        var resultado = evaluador.evaluar(
                def("VALOR", SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "0"),
                Map.of("VALOR", BigDecimal.ZERO));

        assertThat(resultado.valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(resultado.semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(resultado.cumpleMeta()).isTrue();
    }

    @Test
    void mayorMejorConMetaCeroConservaElValorSinSemaforo() {
        var resultado = evaluador.evaluar(
                def("VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "0"),
                Map.of("VALOR", decimal("5")));

        assertThat(resultado.valor()).isEqualByComparingTo("5.00");
        assertThat(resultado.cumplimiento()).isNull();
        assertThat(resultado.semaforo()).isNull();
        assertThat(resultado.noCalculable()).isFalse();
        assertThat(resultado.cumpleMeta()).isTrue();
    }

    @Test
    void unidadPorcentajeEscalaLaRazonUnaSolaVez() {
        var resultado = evaluador.evaluar(
                new DefinicionKpi(
                        "CAMBIOS_CERRADOS / CAMBIOS_TOTAL",
                        SentidoKpi.MAYOR_MEJOR,
                        OperadorMeta.MAYOR_IGUAL,
                        decimal("80"),
                        null,
                        null,
                        null,
                        "%"),
                Map.of("CAMBIOS_CERRADOS", decimal("4"), "CAMBIOS_TOTAL", decimal("5")));

        assertThat(resultado.valor()).isEqualByComparingTo("80.00");
        assertThat(resultado.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(resultado.semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(resultado.cumpleMeta()).isTrue();
    }

    @Test
    void unidadDistintaDePorcentajeNoEscala() {
        var resultado = evaluador.evaluar(
                new DefinicionKpi(
                        "DIAS_ATRASO_CAMBIOS / CAMBIOS_TOTAL",
                        SentidoKpi.MENOR_MEJOR,
                        OperadorMeta.MENOR_IGUAL,
                        decimal("3"),
                        null,
                        null,
                        null,
                        "días"),
                Map.of("DIAS_ATRASO_CAMBIOS", decimal("9"), "CAMBIOS_TOTAL", decimal("3")));

        assertThat(resultado.valor()).isEqualByComparingTo("3.00");
        assertThat(resultado.cumpleMeta()).isTrue();
    }

    @Test
    void porcentajeConDenominadorCeroSigueNoCalculable() {
        var resultado = evaluador.evaluar(
                new DefinicionKpi(
                        "CAMBIOS_CERRADOS / CAMBIOS_TOTAL",
                        SentidoKpi.MAYOR_MEJOR,
                        OperadorMeta.MAYOR_IGUAL,
                        decimal("80"),
                        null,
                        null,
                        null,
                        "%"),
                Map.of("CAMBIOS_CERRADOS", decimal("4"), "CAMBIOS_TOTAL", BigDecimal.ZERO));

        assertNoCalculable(resultado);
    }

    @Test
    void definicionInvalidaOVariableAusenteLanzaError() {
        assertThatThrownBy(() -> def("  ", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefinicionKpi(
                "VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.ENTRE, decimal("1"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefinicionKpi(
                "VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, decimal("1"), null,
                decimal("0.5"), decimal("0.9")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluador.evaluar(
                def("A / B", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1"),
                Map.of("A", decimal("1"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResultadoKpi evaluarValor(String valor, OperadorMeta operador, String meta, String metaMax) {
        return evaluador.evaluar(
                new DefinicionKpi("VALOR", SentidoKpi.MAYOR_MEJOR, operador, decimal(meta),
                        metaMax == null ? null : decimal(metaMax), null, null),
                Map.of("VALOR", decimal(valor)));
    }

    private ResultadoKpi conUmbral(String valor) {
        return evaluador.evaluar(
                new DefinicionKpi("VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, decimal("100"),
                        null, decimal("0.90"), decimal("0.70")),
                Map.of("VALOR", decimal(valor)));
    }

    private ResultadoKpi porcentaje(String valor) {
        return evaluador.evaluar(
                def("VALOR", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "100"),
                Map.of("VALOR", decimal(valor)));
    }

    private static DefinicionKpi def(String formula, SentidoKpi sentido, OperadorMeta operador, String meta) {
        return new DefinicionKpi(formula, sentido, operador, decimal(meta), null, null, null);
    }

    private static void assertNoCalculable(ResultadoKpi resultado) {
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.cumplimiento()).isNull();
        assertThat(resultado.semaforo()).isNull();
        assertThat(resultado.cumpleMeta()).isFalse();
        assertThat(resultado.etiqueta()).isEqualTo("No calculable");
        assertThat(resultado.noCalculable()).isTrue();
    }

    private static BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
