package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;

/**
 * Alerta si el snapshot está en rojo o si su cumplimiento es menor que el anterior.
 * El primer snapshot solo alerta en rojo. No calculable no empeora ni es rojo.
 */
public final class AlertaKpi {

    public static final String ROJO = "ROJO";
    public static final String EMPEORA = "EMPEORA";

    private AlertaKpi() {
    }

    /** {@code null} cuando no hay alerta. Rojo prevalece si además empeoró. */
    public static String motivo(
            SemaforoKpi semaforo, BigDecimal cumplimiento, BigDecimal cumplimientoAnterior) {
        if (semaforo == SemaforoKpi.ROJO) {
            return ROJO;
        }
        if (cumplimiento != null
                && cumplimientoAnterior != null
                && cumplimiento.compareTo(cumplimientoAnterior) < 0) {
            return EMPEORA;
        }
        return null;
    }
}
