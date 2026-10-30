package cl.proyecto.gestion.planificacion.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Calendario laboral. 2026-11-02 es lunes, 2026-11-07 sábado. */
class CalendarioTest {
    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    @Test
    void porDefectoEsLunesAViernes() {
        var cal = new Calendario(Set.of());
        assertTrue(cal.esLaborable(d(6)));
        assertFalse(cal.esLaborable(d(7)));
    }

    @Test
    void diasLaboralesDelProyectoSeLeenDelTexto() {
        var conSabado = Calendario.deProyecto("LMXJVS", Set.of());
        assertTrue(conSabado.esLaborable(d(7)));
        assertFalse(conSabado.esLaborable(d(8)));
        var soloMiercoles = Calendario.deProyecto("X", Set.of());
        assertTrue(soloMiercoles.esLaborable(d(4)));
        assertFalse(soloMiercoles.esLaborable(d(3)));
    }

    @Test
    void sinTextoUsaLunesAViernes() {
        assertFalse(Calendario.deProyecto(null, Set.of()).esLaborable(d(7)));
    }

    @Test
    void feriadoYSabadoLaboralConviven() {
        var cal = Calendario.deProyecto("LMXJVS", Set.of(d(3)));
        assertFalse(cal.esLaborable(d(3)));
        assertEquals(d(7), cal.sumarDiasHabiles(d(5), 2)); // vie 6, sab 7
        assertEquals(5, cal.diasHabilesEntre(d(2), d(7))); // lun, mie, jue, vie, sab (martes feriado)
    }
}
