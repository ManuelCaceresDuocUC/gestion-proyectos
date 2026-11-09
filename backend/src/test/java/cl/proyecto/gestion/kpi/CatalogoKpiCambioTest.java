package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** El catálogo KC coincide con el JSON del seed y se puede evaluar. */
class CatalogoKpiCambioTest {

    private static final Pattern NOMBRE = Pattern.compile("[A-Z][A-Z0-9_]*");
    private final EvaluadorKpi evaluador = new EvaluadorKpi();

    @Test
    void coincideConElJsonDelSeed() throws Exception {
        JsonNode lista = new ObjectMapper().readTree(jsonDelSeed().toFile()).get("kpi_gestion_cambio");

        assertThat(lista).hasSize(13);
        assertThat(CatalogoKpiCambio.todos()).hasSize(13);
        for (JsonNode nodo : lista) {
            CatalogoKpiCambio.Item item = CatalogoKpiCambio.porCodigo(nodo.get("codigo").asText());
            assertThat(item.nombre()).isEqualTo(nodo.get("nombre").asText());
            assertThat(item.formula()).isEqualTo(nodo.get("formula").asText());
            assertThat(item.unidad()).isEqualTo(nodo.get("unidad").asText());
            assertThat(item.sentido().name()).isEqualTo(nodo.get("sentido").asText());
            assertThat(item.operador().name()).isEqualTo(nodo.get("operador_meta").asText());
            assertThat(item.meta()).isEqualByComparingTo(nodo.get("meta").decimalValue());
            if ("%".equals(item.unidad())) {
                assertThat(item.formula()).doesNotContain("* 100");
            }
            Matcher nombres = NOMBRE.matcher(item.formula());
            while (nombres.find()) {
                assertThat(VariablesCambio.nombres()).contains(nombres.group());
            }
        }
    }

    @Test
    void porcentajeUsaLaMetaEnPuntosYDiasNoSeEscala() {
        var cerrados = evaluador.evaluar(
                CatalogoKpiCambio.porCodigo("KC-01").definicion(),
                Map.of("CAMBIOS_CERRADOS", new BigDecimal("8"), "CAMBIOS_TOTAL", new BigDecimal("10")));
        assertThat(cerrados.valor()).isEqualByComparingTo("80.00");
        assertThat(cerrados.cumpleMeta()).isTrue();
        assertThat(cerrados.semaforo()).isEqualTo(SemaforoKpi.VERDE);

        var atraso = evaluador.evaluar(
                CatalogoKpiCambio.porCodigo("KC-12").definicion(),
                Map.of("DIAS_ATRASO_CAMBIOS", new BigDecimal("9"), "CAMBIOS_TOTAL", new BigDecimal("3")));
        assertThat(atraso.valor()).isEqualByComparingTo("3.00");
        assertThat(atraso.cumpleMeta()).isTrue();
    }

    @Test
    void denominadorCeroDelCatalogoEsNoCalculable() {
        var sinCerrados = evaluador.evaluar(
                CatalogoKpiCambio.porCodigo("KC-02").definicion(),
                Map.of("CAMBIOS_CERRADOS_EN_PLAZO", BigDecimal.ZERO, "CAMBIOS_CERRADOS", BigDecimal.ZERO));
        assertThat(sinCerrados.noCalculable()).isTrue();
        assertThat(sinCerrados.etiqueta()).isEqualTo("No calculable");

        var sinPresupuesto = evaluador.evaluar(
                CatalogoKpiCambio.porCodigo("KC-11").definicion(),
                Map.of(
                        "COSTO_REAL_CAMBIOS_CERRADOS", BigDecimal.ZERO,
                        "PRESUPUESTO_CAMBIOS_CERRADOS", BigDecimal.ZERO));
        assertThat(sinPresupuesto.noCalculable()).isTrue();
    }

    private static Path jsonDelSeed() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++) {
            Path candidato = dir.resolve("db").resolve("seed").resolve("kpi-definiciones.json");
            if (Files.exists(candidato)) {
                return candidato;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("No está db/seed/kpi-definiciones.json");
    }
}
