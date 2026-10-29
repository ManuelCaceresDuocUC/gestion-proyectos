package cl.proyecto.gestion.planificacion.recalculo;

/** Orden de ejecución de los pasos. Respétenlo: cada paso depende de los anteriores. */
public final class OrdenRecalculo {
    private OrdenRecalculo() {}
    public static final int MOTOR_FECHAS = 10;     // Dev A: dependencias -> fechas plan
    public static final int CONSOLIDACION = 20;    // Dev A: avance y fechas hacia etapas y proyecto
    public static final int COSTOS = 30;           // Dev B: costo plan/real por asignación y tarea (solo hojas)
    public static final int CONSOLIDACION_COSTOS = 35; // Dev A: suma los costos de las hojas hacia etapas y proyecto
    public static final int SOBREASIGNACION = 40;  // Dev B: alertas de profesionales
    public static final int HITOS = 50;            // Dev A/B: estado y fechas de hitos
    public static final int KPI = 60;              // Dev B: evaluar KPI y guardar histórico
}
