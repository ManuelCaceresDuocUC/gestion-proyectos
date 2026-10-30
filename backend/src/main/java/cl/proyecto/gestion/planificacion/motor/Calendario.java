package cl.proyecto.gestion.planificacion.motor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Calendario laboral: días de la semana laborables menos los días no laborables (feriados).
 * Por defecto lunes a viernes.
 */
public class Calendario {
    private static final String LETRAS = "LMXJVSD"; // lunes, martes, miércoles (X), jueves, viernes, sábado, domingo

    private final Set<DayOfWeek> laborables;
    private final Set<LocalDate> noLaborables;

    public Calendario(Set<LocalDate> noLaborables) {
        this(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), noLaborables);
    }

    public Calendario(Set<DayOfWeek> laborables, Set<LocalDate> noLaborables) {
        if (laborables.isEmpty()) {
            throw new IllegalArgumentException("El calendario necesita al menos un día laborable a la semana.");
        }
        this.laborables = laborables;
        this.noLaborables = noLaborables;
    }

    /** Crea el calendario desde el texto de días del proyecto (por ejemplo "LMXJV"; L M X J V S D). */
    public static Calendario deProyecto(String diasLaborales, Set<LocalDate> noLaborables) {
        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        String texto = diasLaborales == null || diasLaborales.isBlank() ? "LMXJV" : diasLaborales;
        for (char c : texto.toCharArray()) {
            int i = LETRAS.indexOf(c);
            if (i >= 0) dias.add(DayOfWeek.of(i + 1));
        }
        return new Calendario(dias, noLaborables);
    }

    public boolean esLaborable(LocalDate d) {
        return laborables.contains(d.getDayOfWeek()) && !noLaborables.contains(d);
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

    /** Cantidad de días hábiles entre dos fechas, ambas incluidas (0 si fin es anterior a inicio). */
    public int diasHabilesEntre(LocalDate inicio, LocalDate fin) {
        int n = 0;
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            if (esLaborable(d)) n++;
        }
        return n;
    }
}
