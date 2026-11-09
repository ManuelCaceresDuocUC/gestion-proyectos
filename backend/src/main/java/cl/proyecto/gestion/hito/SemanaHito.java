package cl.proyecto.gestion.hito;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import java.time.LocalDate;

/**
 * Semana prevista del hito, contada desde el inicio del proyecto.
 * La semana 1 son los primeros cinco días hábiles; la fecha prevista es el último de esa semana.
 * No consulta la base: el calendario ya viene armado.
 */
public final class SemanaHito {

    static final int DIAS_POR_SEMANA = 5;

    private SemanaHito() {}

    /** Último día hábil de la semana {@code n} (1 = primera semana del proyecto). */
    public static LocalDate fechaDeSemana(LocalDate fechaInicio, Calendario calendario, int semana) {
        if (semana < 1) {
            throw new IllegalArgumentException("La semana prevista empieza en 1.");
        }
        LocalDate inicio = primerLaborable(fechaInicio, calendario);
        return calendario.sumarDiasHabiles(inicio, semana * DIAS_POR_SEMANA - 1);
    }

    /** Semana del proyecto a la que pertenece {@code fecha} (1 si cae en la primera). */
    public static int semanaDe(LocalDate fechaInicio, Calendario calendario, LocalDate fecha) {
        LocalDate inicio = primerLaborable(fechaInicio, calendario);
        if (fecha.isBefore(inicio)) {
            return 1;
        }
        int dias = calendario.diasHabilesEntre(inicio, fecha);
        return ((dias - 1) / DIAS_POR_SEMANA) + 1;
    }

    private static LocalDate primerLaborable(LocalDate fecha, Calendario calendario) {
        if (fecha == null) {
            throw new IllegalArgumentException("El proyecto no tiene fecha de inicio.");
        }
        return calendario.esLaborable(fecha) ? fecha : calendario.sumarDiasHabiles(fecha, 1);
    }
}
