package cl.proyecto.gestion.planificacion.lineabase;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.Tarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Issue A-14: compara base, plan vigente y real por tarea. Lógica pura (sin base de datos).
 * Una tarea está atrasada si terminó después de lo planificado, o si no ha terminado, no está al 100 % y su fecha
 * de fin plan ya pasó ("hoy" se recibe de afuera para poder probarlo).
 */
public class ComparadorPlanes {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public ComparacionPlan comparar(Proyecto proyecto, List<Tarea> tareas, LineaBase ultimaBase,
                                    Calendario calendario, LocalDate hoy) {
        List<Tarea> ordenadas = new ArrayList<>(tareas);
        ordenadas.sort(Comparator.comparing((Tarea t) -> t.codigo, ComparadorPlanes::compararCodigos));
        List<FilaComparacion> filas = new ArrayList<>();
        for (Tarea t : ordenadas) {
            filas.add(fila(t, calendario, hoy));
        }
        ResumenComparacion resumen = resumen(proyecto, tareas, calendario);
        return new ComparacionPlan(ultimaBase == null ? null : ultimaBase.version,
            ultimaBase == null ? null : ultimaBase.creadaEn, resumen, filas);
    }

    private FilaComparacion fila(Tarea t, Calendario cal, LocalDate hoy) {
        boolean atrasada = (t.finReal != null && t.finPlan != null && t.finReal.isAfter(t.finPlan))
            || (t.finReal == null && t.finPlan != null && t.avance.compareTo(CIEN) < 0 && t.finPlan.isBefore(hoy));
        return new FilaComparacion(t.id, t.parentId, t.tipo, t.codigo, t.nombre,
            t.inicioBase, t.finBase, t.inicioPlan, t.finPlan, t.inicioReal, t.finReal,
            diferencia(cal, t.inicioBase, t.inicioPlan),
            diferencia(cal, t.finBase, t.finPlan),
            diferencia(cal, t.finBase, t.finReal),
            t.avance, t.costoBase, t.costoPlan, t.costoReal,
            resta(t.costoPlan, t.costoBase), resta(t.costoReal, t.costoBase), atrasada);
    }

    private ResumenComparacion resumen(Proyecto p, List<Tarea> tareas, Calendario cal) {
        BigDecimal costoBase = null;
        for (Tarea t : tareas) {
            if (t.parentId == null && t.costoBase != null) {
                costoBase = (costoBase == null ? BigDecimal.ZERO : costoBase).add(t.costoBase);
            }
        }
        return new ResumenComparacion(p.fechaFinBase, p.fechaFinPlan, diferencia(cal, p.fechaFinBase, p.fechaFinPlan),
            costoBase, p.costoPlan, p.costoReal, resta(p.costoPlan, costoBase), resta(p.costoReal, costoBase), p.avance);
    }

    /** Días hábiles de "desde" a "hasta": positivo si hasta es posterior. Nulo si falta alguna fecha. */
    static Integer diferencia(Calendario cal, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            return null;
        }
        if (hasta.isAfter(desde)) {
            return cal.diasHabilesEntre(desde.plusDays(1), hasta);
        }
        if (hasta.isBefore(desde)) {
            return -cal.diasHabilesEntre(hasta.plusDays(1), desde);
        }
        return 0;
    }

    private static BigDecimal resta(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? null : a.subtract(b);
    }

    /** Orden natural de códigos EDT: 1.2 antes que 1.10. */
    static int compararCodigos(String x, String y) {
        String[] a = x.split("\\.");
        String[] b = y.split("\\.");
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int c = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (c != 0) return c;
        }
        return Integer.compare(a.length, b.length);
    }
}
