package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.proyecto.Tarea;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Foto de las fechas plan de un proyecto antes de una operación. Después del recálculo, cambiosHasta() lista
 * las tareas cuyas fechas cambiaron (así la API responde qué se movió).
 */
public class InstantaneaFechas {

    private record Foto(String codigo, String nombre, LocalDate inicio, LocalDate fin) {}

    private final Map<Long, Foto> antes = new HashMap<>();

    public static InstantaneaFechas tomar(List<Tarea> tareas) {
        InstantaneaFechas f = new InstantaneaFechas();
        for (Tarea t : tareas) {
            f.antes.put(t.id, new Foto(t.codigo, t.nombre, t.inicioPlan, t.finPlan));
        }
        return f;
    }

    public List<CambioFecha> cambiosHasta(List<Tarea> despues) {
        List<CambioFecha> cambios = new ArrayList<>();
        for (Tarea t : despues) {
            Foto a = antes.get(t.id);
            LocalDate inicioAntes = a == null ? null : a.inicio();
            LocalDate finAntes = a == null ? null : a.fin();
            if (!Objects.equals(inicioAntes, t.inicioPlan) || !Objects.equals(finAntes, t.finPlan)) {
                cambios.add(new CambioFecha(t.id, t.codigo, t.nombre, inicioAntes, finAntes, t.inicioPlan, t.finPlan));
            }
        }
        cambios.sort(Comparator.comparing(CambioFecha::codigo, InstantaneaFechas::compararCodigos));
        return cambios;
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
