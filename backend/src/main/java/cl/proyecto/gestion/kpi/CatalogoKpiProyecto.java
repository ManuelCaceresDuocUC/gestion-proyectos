package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * KP-01…KP-08 tal como están en {@code db/seed/kpi-definiciones.json}.
 * Las de unidad {@code %} son razones: el evaluador aplica el × 100.
 * KP-01 no trae meta numérica: la meta efectiva es {@code AVANCE_PLAN} en puntos porcentuales.
 */
public final class CatalogoKpiProyecto {

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

        /**
         * Meta guardada, o {@code AVANCE_PLAN × 100} si la meta del seed es nula.
         * {@code null} si esa variable no está y no hay meta numérica.
         */
        public DefinicionKpi definicionEfectiva(Map<String, BigDecimal> variables) {
            BigDecimal metaEfectiva = meta;
            if (metaEfectiva == null) {
                BigDecimal plan = variables == null ? null : variables.get(VariablesProyecto.AVANCE_PLAN);
                if (plan == null) {
                    return null;
                }
                metaEfectiva = plan.multiply(new BigDecimal("100"));
            }
            return new DefinicionKpi(formula, sentido, operador, metaEfectiva, null, null, null, unidad);
        }
    }

    private static final List<Item> ITEMS = List.of(
            item("KP-01", "Avance físico real", CategoriaKpi.PROYECTO,
                    "AVANCE_REAL", "%", SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, null,
                    "Coordinación",
                    "Avance ponderado consolidado desde la EDT. La meta es el avance planificado a la fecha (AVANCE_PLAN)."),
            item("KP-02", "Índice de desempeño del cronograma (SPI)", CategoriaKpi.PROYECTO,
                    "VALOR_GANADO / VALOR_PLANIFICADO", "índice",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1.0",
                    "Coordinación", "EV/PV. Menor que 1 indica atraso."),
            item("KP-03", "Índice de desempeño del costo (CPI)", CategoriaKpi.PROYECTO,
                    "VALOR_GANADO / COSTO_REAL", "índice",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "1.0",
                    "Administración", "EV/AC. Menor que 1 indica sobrecosto."),
            item("KP-04", "Desviación de plazo", CategoriaKpi.PROYECTO,
                    "DIAS_ATRASO_PROYECTO", "días",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "0",
                    "Coordinación", "Días entre fin planificado actual y fin de la línea base."),
            item("KP-05", "% de actividades atrasadas", CategoriaKpi.PROYECTO,
                    "ACTIVIDADES_ATRASADAS / ACTIVIDADES_TOTAL", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "15",
                    "Coordinación", "Actividades hoja con fin previsto vencido y avance menor a 100%."),
            item("KP-06", "% de hitos cumplidos a tiempo", CategoriaKpi.HITOS,
                    "HITOS_CUMPLIDOS_A_TIEMPO / HITOS_VENCIDOS", "%",
                    SentidoKpi.MAYOR_MEJOR, OperadorMeta.MAYOR_IGUAL, "90",
                    "Dirección", "Sobre los hitos cuya fecha ya venció."),
            item("KP-07", "Desviación de costo", CategoriaKpi.PROYECTO,
                    "(COSTO_REAL - COSTO_PLAN_A_LA_FECHA) / COSTO_PLAN_A_LA_FECHA", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "5",
                    "Administración", "Positivo = gasto sobre lo planificado."),
            item("KP-08", "% de profesionales sobreasignados", CategoriaKpi.PROYECTO,
                    "PROFESIONALES_SOBREASIGNADOS / PROFESIONALES_TOTAL", "%",
                    SentidoKpi.MENOR_MEJOR, OperadorMeta.MENOR_IGUAL, "0",
                    "Coordinación", "Profesionales con carga mayor a su capacidad diaria en algún día."));

    private static final Map<String, Item> POR_CODIGO = ITEMS.stream()
            .collect(Collectors.toUnmodifiableMap(Item::codigo, Function.identity()));

    private CatalogoKpiProyecto() {
    }

    public static List<Item> todos() {
        return ITEMS;
    }

    public static Item porCodigo(String codigo) {
        Item item = POR_CODIGO.get(codigo);
        if (item == null) {
            throw new IllegalArgumentException("No existe el KPI de proyecto " + codigo + ".");
        }
        return item;
    }

    private static Item item(
            String codigo,
            String nombre,
            CategoriaKpi categoria,
            String formula,
            String unidad,
            SentidoKpi sentido,
            OperadorMeta operador,
            String meta,
            String responsable,
            String descripcion) {
        return new Item(
                codigo, nombre, categoria, formula, unidad, sentido, operador,
                meta == null ? null : new BigDecimal(meta), responsable, descripcion);
    }
}
