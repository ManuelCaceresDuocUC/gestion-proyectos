package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;

/**
 * Valor a 2 decimales, cumplimiento a 4 (tope 1) y semáforo.
 * Valor nulo significa «No calculable»: sin cumplimiento ni semáforo.
 */
public record ResultadoKpi(
        BigDecimal valor,
        BigDecimal cumplimiento,
        SemaforoKpi semaforo,
        boolean cumpleMeta
) {
    public static final String ETIQUETA_NO_CALCULABLE = "No calculable";

    public boolean noCalculable() {
        return valor == null;
    }

    /** «No calculable» cuando el valor es nulo; si no, null. */
    public String etiqueta() {
        return noCalculable() ? ETIQUETA_NO_CALCULABLE : null;
    }

    public static ResultadoKpi sinValor() {
        return new ResultadoKpi(null, null, null, false);
    }
}
