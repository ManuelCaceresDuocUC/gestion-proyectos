package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** El catálogo KP-01…KP-08 coincide con el JSON del seed y se evalúa con esos valores. */
class CatalogoKpiProyectoTest {

    private final EvaluadorKpi evaluador = new EvaluadorKpi();

    @Test
    void coincideConElJsonDelSeed() throws Exception {
        JsonNode lista = new ObjectMapper().readTree(jsonDelSeed().toFile()).get("kpi_proyecto_hitos");

        assertThat(lista).hasSize(8);
        assertThat(CatalogoKpiProyecto.todos()).hasSize(8);
        for (JsonNode nodo : lista) {
            CatalogoKpiProyecto.Item item = CatalogoKpiProyecto.porCodigo(nodo.get("codigo").asText());
            assertThat(item.nombre()).isEqualTo(nodo.get("nombre").asText());
            assertThat(item.categoria().name()).isEqualTo(nodo.get("categoria").asText());
            assertThat(item.formula()).isEqualTo(nodo.get("formula").asText());
            assertThat(item.unidad()).isEqualTo(nodo.get("unidad").asText());
            assertThat(item.sentido().name()).isEqualTo(nodo.get("sentido").asText());
            assertThat(item.operador().name()).isEqualTo(nodo.get("operador_meta").asText());
            assertThat(item.responsable()).isEqualTo(nodo.get("responsable").asText());
            assertThat(item.descripcion()).isEqualTo(nodo.get("descripcion").asText());
            assertThat(nodo.get("activo").asBoolean()).isTrue();
            if (nodo.get("meta").isNull()) {
                assertThat(item.meta()).isNull();
            } else {
                assertThat(item.meta()).isEqualByComparingTo(nodo.get("meta").decimalValue());
            }
            if ("%".equals(item.unidad())) {
                assertThat(item.formula()).doesNotContain("* 100");
            }
            for (String nombre : FormulaKpi.nombres(item.formula())) {
                assertThat(VariablesProyecto.nombres()).contains(nombre);
            }
        }
    }

    @Test
    void evaluaLosOchoConLaMetaDinamicaDeKp01() {
        Map<String, BigDecimal> variables = new HashMap<>();
        variables.put(VariablesProyecto.AVANCE_REAL, new BigDecimal("0.40"));
        variables.put(VariablesProyecto.AVANCE_PLAN, new BigDecimal("0.50"));
        variables.put(VariablesProyecto.VALOR_GANADO, new BigDecimal("400"));
        variables.put(VariablesProyecto.VALOR_PLANIFICADO, new BigDecimal("500"));
        variables.put(VariablesProyecto.COSTO_REAL, new BigDecimal("400"));
        variables.put(VariablesProyecto.COSTO_PLAN_A_LA_FECHA, new BigDecimal("500"));
        variables.put(VariablesProyecto.DIAS_ATRASO_PROYECTO, BigDecimal.ZERO);
        variables.put(VariablesProyecto.ACTIVIDADES_ATRASADAS, BigDecimal.ZERO);
        variables.put(VariablesProyecto.ACTIVIDADES_TOTAL, new BigDecimal("4"));
        variables.put(VariablesProyecto.HITOS_CUMPLIDOS_A_TIEMPO, new BigDecimal("9"));
        variables.put(VariablesProyecto.HITOS_VENCIDOS, new BigDecimal("10"));
        variables.put(VariablesProyecto.PROFESIONALES_SOBREASIGNADOS, BigDecimal.ZERO);
        variables.put(VariablesProyecto.PROFESIONALES_TOTAL, new BigDecimal("3"));

        ResultadoKpi avance = evaluar("KP-01", variables);
        assertThat(avance.valor()).isEqualByComparingTo("40.00");
        assertThat(avance.cumplimiento()).isEqualByComparingTo("0.8000");
        assertThat(avance.semaforo()).isEqualTo(SemaforoKpi.AMARILLO);
        assertThat(avance.cumpleMeta()).isFalse();

        ResultadoKpi spi = evaluar("KP-02", variables);
        assertThat(spi.valor()).isEqualByComparingTo("0.80");
        assertThat(spi.semaforo()).isEqualTo(SemaforoKpi.AMARILLO);

        ResultadoKpi cpi = evaluar("KP-03", variables);
        assertThat(cpi.valor()).isEqualByComparingTo("1.00");
        assertThat(cpi.semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(cpi.cumpleMeta()).isTrue();

        ResultadoKpi plazo = evaluar("KP-04", variables);
        assertThat(plazo.valor()).isEqualByComparingTo("0.00");
        assertThat(plazo.cumplimiento()).isEqualByComparingTo("1.0000");
        assertThat(plazo.semaforo()).isEqualTo(SemaforoKpi.VERDE);

        ResultadoKpi atrasadas = evaluar("KP-05", variables);
        assertThat(atrasadas.valor()).isEqualByComparingTo("0.00");
        assertThat(atrasadas.semaforo()).isEqualTo(SemaforoKpi.VERDE);

        ResultadoKpi hitos = evaluar("KP-06", variables);
        assertThat(hitos.valor()).isEqualByComparingTo("90.00");
        assertThat(hitos.cumpleMeta()).isTrue();
        assertThat(hitos.semaforo()).isEqualTo(SemaforoKpi.VERDE);

        ResultadoKpi costo = evaluar("KP-07", variables);
        assertThat(costo.valor()).isEqualByComparingTo("-20.00");
        assertThat(costo.cumpleMeta()).isTrue();

        ResultadoKpi carga = evaluar("KP-08", variables);
        assertThat(carga.valor()).isEqualByComparingTo("0.00");
        assertThat(carga.semaforo()).isEqualTo(SemaforoKpi.VERDE);
        assertThat(carga.cumpleMeta()).isTrue();
    }

    @Test
    void denominadorCeroDelCatalogoEsNoCalculable() {
        Map<String, BigDecimal> ceros = Map.of(
                VariablesProyecto.VALOR_GANADO, BigDecimal.ZERO,
                VariablesProyecto.VALOR_PLANIFICADO, BigDecimal.ZERO,
                VariablesProyecto.COSTO_REAL, BigDecimal.ZERO,
                VariablesProyecto.COSTO_PLAN_A_LA_FECHA, BigDecimal.ZERO,
                VariablesProyecto.ACTIVIDADES_ATRASADAS, BigDecimal.ZERO,
                VariablesProyecto.ACTIVIDADES_TOTAL, BigDecimal.ZERO,
                VariablesProyecto.HITOS_CUMPLIDOS_A_TIEMPO, BigDecimal.ZERO,
                VariablesProyecto.HITOS_VENCIDOS, BigDecimal.ZERO,
                VariablesProyecto.PROFESIONALES_SOBREASIGNADOS, BigDecimal.ZERO,
                VariablesProyecto.PROFESIONALES_TOTAL, BigDecimal.ZERO);

        assertThat(evaluar("KP-02", ceros).noCalculable()).isTrue();
        assertThat(evaluar("KP-05", ceros).noCalculable()).isTrue();
        assertThat(evaluar("KP-06", ceros).noCalculable()).isTrue();
        assertThat(evaluar("KP-07", ceros).noCalculable()).isTrue();
        assertThat(evaluar("KP-08", ceros).noCalculable()).isTrue();
        assertThat(evaluar("KP-02", ceros).etiqueta()).isEqualTo("No calculable");
    }

    private ResultadoKpi evaluar(String codigo, Map<String, BigDecimal> variables) {
        DefinicionKpi definicion = CatalogoKpiProyecto.porCodigo(codigo).definicionEfectiva(variables);
        assertThat(definicion).isNotNull();
        return evaluador.evaluar(definicion, variables);
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
