package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;

/**
 * Qué hay que evaluar: fórmula en texto, sentido, operador de meta y umbrales.
 * No conoce proyecto ni base de datos. Los umbrales nulos usan 1 y 0.8.
 * {@code unidad} nula no cambia la escala. {@code "%"} pide al evaluador pasar la razón a puntos porcentuales.
 */
public record DefinicionKpi(
        String formula,
        SentidoKpi sentido,
        OperadorMeta operador,
        BigDecimal meta,
        BigDecimal metaMax,
        BigDecimal umbralVerde,
        BigDecimal umbralAmarillo,
        String unidad
) {
    public DefinicionKpi(
            String formula,
            SentidoKpi sentido,
            OperadorMeta operador,
            BigDecimal meta,
            BigDecimal metaMax,
            BigDecimal umbralVerde,
            BigDecimal umbralAmarillo) {
        this(formula, sentido, operador, meta, metaMax, umbralVerde, umbralAmarillo, null);
    }

    public DefinicionKpi {
        if (formula == null || formula.isBlank()) {
            throw new IllegalArgumentException("La fórmula del KPI es obligatoria.");
        }
        if (sentido == null) {
            throw new IllegalArgumentException("El sentido del KPI es obligatorio.");
        }
        if (operador == null) {
            throw new IllegalArgumentException("El operador de meta es obligatorio.");
        }
        if (meta == null) {
            throw new IllegalArgumentException("La meta del KPI es obligatoria.");
        }
        if (operador == OperadorMeta.ENTRE && metaMax == null) {
            throw new IllegalArgumentException("ENTRE requiere meta y meta máxima.");
        }
        if (umbralVerde == null) {
            umbralVerde = BigDecimal.ONE;
        }
        if (umbralAmarillo == null) {
            umbralAmarillo = new BigDecimal("0.8");
        }
        if (umbralAmarillo.compareTo(umbralVerde) > 0) {
            throw new IllegalArgumentException("El umbral amarillo no puede superar al verde.");
        }
        if (unidad != null && unidad.isBlank()) {
            unidad = null;
        }
    }

    /** La fórmula es una razón y el resultado se expresa en puntos porcentuales. */
    public boolean esPorcentaje() {
        return "%".equals(unidad);
    }
}
