package cl.proyecto.gestion.planificacion.lineabase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TipoTarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A-14. Lógica pura: no necesita base de datos. 2026-11-02 es lunes. */
class ComparadorPlanesTest {
    private final ComparadorPlanes comparador = new ComparadorPlanes();
    private final Calendario cal = new Calendario(Set.of());

    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    static Tarea tarea(long id, String codigo, LocalDate inicioBase, LocalDate finBase,
                       LocalDate inicioPlan, LocalDate finPlan, String costoBase, String costoPlan, String costoReal) {
        Tarea t = new Tarea();
        t.id = id;
        t.codigo = codigo;
        t.nombre = "T" + codigo;
        t.tipo = TipoTarea.ACTIVIDAD;
        t.inicioBase = inicioBase;
        t.finBase = finBase;
        t.inicioPlan = inicioPlan;
        t.finPlan = finPlan;
        t.costoBase = costoBase == null ? null : new BigDecimal(costoBase);
        t.costoPlan = new BigDecimal(costoPlan);
        t.costoReal = new BigDecimal(costoReal);
        t.avance = BigDecimal.ZERO;
        return t;
    }

    FilaComparacion unica(Tarea t, LocalDate hoy) {
        return comparador.comparar(new Proyecto(), List.of(t), null, cal, hoy).tareas().get(0);
    }

    @Test
    void sinLineaBaseLasDesviacionesSonNulas() {
        var t = tarea(1, "1", null, null, d(2), d(4), null, "100", "0");
        var r = comparador.comparar(new Proyecto(), List.of(t), null, cal, d(2));
        assertNull(r.versionLineaBase());
        assertNull(r.tareas().get(0).desviacionFinDias());
        assertNull(r.tareas().get(0).desviacionCostoPlan());
    }

    @Test
    void unPlanAtrasadoTieneDesviacionPositivaEnDiasHabiles() {
        var f = unica(tarea(1, "1", d(2), d(4), d(2), d(11), "100", "100", "0"), d(2));
        assertEquals(0, f.desviacionInicioDias());
        assertEquals(5, f.desviacionFinDias()); // jue 5, vie 6, lun 9, mar 10, mie 11
    }

    @Test
    void unPlanAdelantadoTieneDesviacionNegativa() {
        var f = unica(tarea(1, "1", d(2), d(11), d(2), d(4), "100", "100", "0"), d(2));
        assertEquals(-5, f.desviacionFinDias());
    }

    @Test
    void desviacionDeCostosContraLaBase() {
        var f = unica(tarea(1, "1", d(2), d(4), d(2), d(4), "100", "150", "170"), d(2));
        assertEquals(0, new BigDecimal("50").compareTo(f.desviacionCostoPlan()));
        assertEquals(0, new BigDecimal("70").compareTo(f.desviacionCostoReal()));
    }

    @Test
    void desviacionDeFinRealContraLaBase() {
        var t = tarea(1, "1", d(2), d(4), d(2), d(4), "100", "100", "0");
        t.finReal = d(6);
        assertEquals(2, unica(t, d(10)).desviacionFinRealDias()); // jue 5, vie 6
    }

    @Test
    void marcaLasAtrasadas() {
        var vencida = tarea(1, "1", d(2), d(6), d(2), d(6), "0", "0", "0");
        vencida.avance = new BigDecimal("50");
        assertTrue(unica(vencida, d(9)).atrasada());      // fin plan vencido y sin terminar
        assertFalse(unica(vencida, d(5)).atrasada());     // aun no vence

        var terminada = tarea(2, "2", d(2), d(6), d(2), d(6), "0", "0", "0");
        terminada.avance = new BigDecimal("100");
        assertFalse(unica(terminada, d(9)).atrasada());   // al 100 % no esta atrasada

        var terminoTarde = tarea(3, "3", d(2), d(6), d(2), d(6), "0", "0", "0");
        terminoTarde.avance = new BigDecimal("100");
        terminoTarde.finReal = d(9);
        assertTrue(unica(terminoTarde, d(9)).atrasada()); // termino despues de lo planificado
    }

    @Test
    void unaTareaCreadaDespuesDeLaBaseNoFalla() {
        var nueva = tarea(1, "1", null, null, d(2), d(4), null, "100", "0");
        var f = unica(nueva, d(2));
        assertNull(f.inicioBase());
        assertNull(f.desviacionInicioDias());
    }

    @Test
    void ordenaPorCodigoNaturalYArmaElResumen() {
        var t2 = tarea(2, "1.2", d(2), d(4), d(2), d(4), "10", "10", "0");
        var t10 = tarea(3, "1.10", d(2), d(4), d(2), d(4), "10", "10", "0");
        var etapa = tarea(1, "1", d(2), d(4), d(2), d(11), "100", "130", "40");
        etapa.parentId = null;
        t2.parentId = 1L;
        t10.parentId = 1L;
        var p = new Proyecto();
        p.fechaFinBase = d(4);
        p.fechaFinPlan = d(11);
        p.costoPlan = new BigDecimal("130");
        p.costoReal = new BigDecimal("40");
        p.avance = new BigDecimal("25");
        var lb = new LineaBase();
        lb.version = 2;

        var r = comparador.comparar(p, List.of(t10, t2, etapa), lb, cal, d(2));
        assertEquals(List.of("1", "1.2", "1.10"), r.tareas().stream().map(FilaComparacion::codigo).toList());
        assertEquals(2, r.versionLineaBase());
        assertEquals(5, r.resumen().desviacionFinDias());
        assertEquals(0, new BigDecimal("100").compareTo(r.resumen().costoBase())); // solo suma las etapas
        assertEquals(0, new BigDecimal("30").compareTo(r.resumen().desviacionCostoPlan()));
    }
}
