package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * KC-01…KC-13 tal como están en {@code db/seed/kpi-definiciones.json}.
 * Las de unidad {@code %} son razones: el evaluador aplica el × 100.
 * KC-11…KC-13 siguen siendo la propuesta del JSON hasta que el profesor indique otras.
 */
public final class CatalogoKpiCambio {

    public record Item(
            String codigo,
            String nombre,
            CategoriaKpi categoria,
            String formula,
            String unidad,
            SentidoKpi sentido,
            OperadorMeta operador,
            BigDecimal meta,
            String responsable,
            String descripcion) {

        public DefinicionKpi definicion() {
            return new DefinicionKpi(formula, sentido, operador, meta, null, null, null, unidad);
        }
    }

    private static final List<Item> ITEMS = List.of(
            item("KC-01", "% cambios cerrados",
                    "CAMBIOS_CERRADOS / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "80",
                    "Gerencia", "Plantilla KPI-01"),
            item("KC-02", "% cambios dentro de plazo",
                    "CAMBIOS_CERRADOS_EN_PLAZO / CAMBIOS_CERRADOS", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "90",
                    "PMO", "Plantilla KPI-02"),
            item("KC-03", "% cambios de alto riesgo",
                    "CAMBIOS_RIESGO_ALTO / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "25",
                    "Gestión de riesgos", "Plantilla KPI-03 (riesgo Alto o Crítico)"),
            item("KC-04", "% cambios con atraso",
                    "CAMBIOS_CON_ATRASO / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "15",
                    "PMO", "Plantilla KPI-04"),
            item("KC-05", "% cambios que exceden presupuesto",
                    "CAMBIOS_CERRADOS_CON_SOBRECOSTO / CAMBIOS_CERRADOS_CON_PRESUPUESTO", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "10",
                    "Finanzas", "Plantilla KPI-05"),
            item("KC-06", "% etapas completadas",
                    "ETAPAS_COMPLETADAS / ETAPAS_TOTALES", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "75",
                    "Líderes de cambio", "Plantilla KPI-06 (ETAPAS_TOTALES = 10 x cambios seguidos)"),
            item("KC-07", "% adopción cultural",
                    "CAMBIOS_ADOPTADOS / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "70",
                    "RR.HH.", "Plantilla KPI-07 (etapa 10 completada)"),
            item("KC-08", "% cambios de prioridad alta o crítica",
                    "CAMBIOS_PRIORIDAD_ALTA / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "35",
                    "Gerencia", "Plantilla KPI-08"),
            item("KC-09", "% cambios con etapas bloqueadas",
                    "CAMBIOS_CON_BLOQUEO / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "10",
                    "PMO", "Plantilla KPI-09"),
            item("KC-10", "% cambios con acciones correctivas",
                    "CAMBIOS_CON_CORRECTIVA / CAMBIOS_TOTAL", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "50",
                    "Calidad", "Plantilla KPI-10"),
            item("KC-11", "Desviación presupuestaria de cambios cerrados",
                    "(COSTO_REAL_CAMBIOS_CERRADOS - PRESUPUESTO_CAMBIOS_CERRADOS) / PRESUPUESTO_CAMBIOS_CERRADOS",
                    "%", SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "5",
                    "Finanzas", "ADICIONAL propuesto"),
            item("KC-12", "Atraso promedio por cambio",
                    "DIAS_ATRASO_CAMBIOS / CAMBIOS_TOTAL", "días",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "3",
                    "PMO", "ADICIONAL propuesto"),
            item("KC-13", "% cambios aprobados ya aplicados al plan",
                    "CAMBIOS_APROBADOS_APLICADOS / CAMBIOS_APROBADOS", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "100",
                    "Coordinación",
                    "ADICIONAL propuesto: mide la propagación aprobación -> plan/costos/KPI"));

    private static final Map<String, Item> POR_CODIGO = ITEMS.stream()
            .collect(Collectors.toUnmodifiableMap(Item::codigo, Function.identity()));

    private CatalogoKpiCambio() {
    }

    public static List<Item> todos() {
        return ITEMS;
    }

    public static Item porCodigo(String codigo) {
        Item item = POR_CODIGO.get(codigo);
        if (item == null) {
            throw new IllegalArgumentException("No existe el KPI de cambio " + codigo + ".");
        }
        return item;
    }

    private static Item item(
            String codigo,
            String nombre,
            String formula,
            String unidad,
            SentidoKpi sentido,
            OperadorMeta operador,
            String meta,
            String responsable,
            String descripcion) {
        return new Item(
                codigo, nombre, CategoriaKpi.CAMBIO, formula, unidad, sentido, operador,
                new BigDecimal(meta), responsable, descripcion);
    }
}
