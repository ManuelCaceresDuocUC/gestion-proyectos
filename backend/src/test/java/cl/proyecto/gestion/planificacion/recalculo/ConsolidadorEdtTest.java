package cl.proyecto.gestion.planificacion.recalculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.Tarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A-06. Lógica pura: no necesita base de datos. 2026-11-02 es lunes. */
class ConsolidadorEdtTest {
    private final ConsolidadorEdt consolidador = new ConsolidadorEdt();
    private final Calendario cal = new Calendario(Set.of());

    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    static Tarea hoja(long id, Long padre, int orden, int dur, String avance, String costoPlan, String costoReal,
                      LocalDate inicio, LocalDate fin) {
        Tarea t = padre(id, padre, orden);
        t.duracionDias = dur;
        t.avance = new BigDecimal(avance);
        t.costoPlan = new BigDecimal(costoPlan);
        t.costoReal = new BigDecimal(costoReal);
        t.inicioPlan = inicio;
        t.finPlan = fin;
        return t;
    }

    static Tarea padre(long id, Long padre, int orden) {
        Tarea t = new Tarea();
        t.id = id;
        t.parentId = padre;
        t.orden = orden;
        return t;
    }

    /** Etapa 1 > (Actividad 1.1 > Sub 1.1.1, Sub 1.1.2), Actividad 1.2. */
    List<Tarea> arbol() {
        List<Tarea> l = new ArrayList<>();
        l.add(padre(1, null, 1));
        l.add(padre(2, 1L, 1));
        l.add(hoja(3, 2L, 1, 2, "100", "100", "50", d(2), d(3)));
        l.add(hoja(4, 2L, 2, 3, "0", "200", "0", d(4), d(6)));
        l.add(hoja(5, 1L, 2, 5, "50", "300", "100", d(2), d(6)));
        return l;
    }

    @Test
    void consolidaFechasAvanceYCostosHastaElProyecto() {
        var tareas = arbol();
        var proyecto = new Proyecto();
        consolidador.aplicar(proyecto, tareas, cal);

        Tarea act = tareas.get(1);
        assertEquals(d(2), act.inicioPlan);
        assertEquals(d(6), act.finPlan);
        assertEquals(5, act.duracionDias);
        assertEquals(new BigDecimal("40.00"), act.avance); // (100*2 + 0*3) / 5
        assertEquals(0, new BigDecimal("300").compareTo(act.costoPlan));
        assertEquals(0, new BigDecimal("50").compareTo(act.costoReal));

        Tarea etapa = tareas.get(0);
        assertEquals(new BigDecimal("45.00"), etapa.avance); // (40*5 + 50*5) / 10
        assertEquals(0, new BigDecimal("600").compareTo(etapa.costoPlan));
        assertEquals(0, new BigDecimal("150").compareTo(etapa.costoReal));
        assertEquals(d(2), etapa.inicioPlan);
        assertEquals(d(6), etapa.finPlan);

        assertEquals(new BigDecimal("45.00"), proyecto.avance);
        assertEquals(0, new BigDecimal("600").compareTo(proyecto.costoPlan));
        assertEquals(0, new BigDecimal("150").compareTo(proyecto.costoReal));
        assertEquals(d(6), proyecto.fechaFinPlan);
    }

    @Test
    void esIdempotenteYNoDuplicaCostos() {
        var tareas = arbol();
        var proyecto = new Proyecto();
        consolidador.aplicar(proyecto, tareas, cal);
        consolidador.aplicar(proyecto, tareas, cal);
        consolidador.aplicar(proyecto, tareas, cal);
        assertEquals(0, new BigDecimal("600").compareTo(proyecto.costoPlan));
        assertEquals(0, new BigDecimal("600").compareTo(tareas.get(0).costoPlan));
    }

    @Test
    void duracionDelPadreCuentaSoloDiasHabiles() {
        var tareas = new ArrayList<Tarea>();
        tareas.add(padre(1, null, 1));
        tareas.add(hoja(2, 1L, 1, 3, "0", "0", "0", d(2), d(4)));
        tareas.add(hoja(3, 1L, 2, 3, "0", "0", "0", d(6), d(10)));
        consolidador.aplicar(new Proyecto(), tareas, cal);
        assertEquals(7, tareas.get(0).duracionDias); // lun 2 a mar 10 = 7 habiles
    }

    @Test
    void conTodasLasDuracionesEnCeroUsaPromedioSimple() {
        var tareas = new ArrayList<Tarea>();
        tareas.add(padre(1, null, 1));
        tareas.add(hoja(2, 1L, 1, 0, "100", "0", "0", d(2), d(2)));
        tareas.add(hoja(3, 1L, 2, 0, "0", "0", "0", d(2), d(2)));
        consolidador.aplicar(new Proyecto(), tareas, cal);
        assertEquals(new BigDecimal("50.00"), tareas.get(0).avance);
    }

    @Test
    void proyectoSinTareasQuedaEnCero() {
        var proyecto = new Proyecto();
        consolidador.aplicar(proyecto, List.of(), cal);
        assertEquals(0, BigDecimal.ZERO.compareTo(proyecto.avance));
        assertEquals(0, BigDecimal.ZERO.compareTo(proyecto.costoPlan));
        assertNull(proyecto.fechaFinPlan);
    }
}
