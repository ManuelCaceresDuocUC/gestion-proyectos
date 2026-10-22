package cl.proyecto.gestion.planificacion.motor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Calendario laboral. La implementación por defecto es lunes a viernes menos los feriados indicados. */
public class Calendario {
    private final Set<LocalDate> noLaborables;

    public Calendario(Set<LocalDate> noLaborables) {
        this.noLaborables = noLaborables;
    }

    public boolean esLaborable(LocalDate d) {
        return d.getDayOfWeek() != DayOfWeek.SATURDAY
            && d.getDayOfWeek() != DayOfWeek.SUNDAY
            && !noLaborables.contains(d);
    }

    /** Suma (o resta, si n es negativo) n días hábiles. */
    public LocalDate sumarDiasHabiles(LocalDate d, int n) {
        int paso = n >= 0 ? 1 : -1;
        int pendientes = Math.abs(n);
        LocalDate r = d;
        while (pendientes > 0) {
            r = r.plusDays(paso);
            if (esLaborable(r)) pendientes--;
        }
        return r;
    }
}
