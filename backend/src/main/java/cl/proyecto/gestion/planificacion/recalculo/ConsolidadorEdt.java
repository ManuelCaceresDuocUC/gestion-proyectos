package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.Tarea;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Issue A-06: consolida hacia arriba (hojas -> actividades -> etapas -> proyecto). Lógica pura, sin base de datos.
 * Reglas (ver ADR-003):
 *  - Fechas de un padre: inicio = mínimo y fin = máximo de sus hijos. Su duración = días hábiles entre ambas.
 *  - Avance de un padre: promedio ponderado por la duración de cada hijo (si todos duran 0, promedio simple).
 *  - Costos de un padre: suma de los hijos. Solo las hojas tienen costo propio, así que nada se cuenta dos veces.
 *  - El proyecto: avance ponderado de sus etapas, costos sumados y fecha de fin plan = fin más tardío.
 * Es idempotente: se puede aplicar las veces que haga falta sobre los mismos datos.
 */
public class ConsolidadorEdt {

    public void aplicar(Proyecto proyecto, List<Tarea> tareas, Calendario calendario) {
        Map<Long, List<Tarea>> hijos = new HashMap<>();
        List<Tarea> etapas = new ArrayList<>();
        for (Tarea t : tareas) {
            if (t.parentId == null) {
                etapas.add(t);
            } else {
                hijos.computeIfAbsent(t.parentId, k -> new ArrayList<>()).add(t);
            }
        }
        etapas.sort(Comparator.comparingInt(t -> t.orden));
        for (Tarea e : etapas) {
            consolidar(e, hijos, calendario);
        }

        proyecto.avance = promedioPonderado(etapas);
        proyecto.costoPlan = etapas.stream().map(t -> t.costoPlan).reduce(BigDecimal.ZERO, BigDecimal::add);
        proyecto.costoReal = etapas.stream().map(t -> t.costoReal).reduce(BigDecimal.ZERO, BigDecimal::add);
        proyecto.fechaFinPlan = etapas.stream().map(t -> t.finPlan).filter(f -> f != null)
            .max(Comparator.naturalOrder()).orElse(null);
    }

    private void consolidar(Tarea t, Map<Long, List<Tarea>> hijos, Calendario cal) {
        List<Tarea> hs = hijos.getOrDefault(t.id, List.of());
        if (hs.isEmpty()) {
            return; // hoja: conserva sus propios valores
        }
        for (Tarea h : hs) {
            consolidar(h, hijos, cal);
        }
        t.inicioPlan = hs.stream().map(h -> h.inicioPlan).filter(f -> f != null).min(Comparator.naturalOrder()).orElse(null);
        t.finPlan = hs.stream().map(h -> h.finPlan).filter(f -> f != null).max(Comparator.naturalOrder()).orElse(null);
        t.duracionDias = (t.inicioPlan == null || t.finPlan == null) ? null
            : cal.diasHabilesEntre(t.inicioPlan, t.finPlan);
        t.avance = promedioPonderado(hs);
        t.costoPlan = hs.stream().map(h -> h.costoPlan).reduce(BigDecimal.ZERO, BigDecimal::add);
        t.costoReal = hs.stream().map(h -> h.costoReal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Promedio del avance ponderado por duración; con peso total 0 usa promedio simple; sin elementos, 0. */
    static BigDecimal promedioPonderado(List<Tarea> ts) {
        if (ts.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal suma = BigDecimal.ZERO;
        BigDecimal pesos = BigDecimal.ZERO;
        for (Tarea t : ts) {
            BigDecimal peso = BigDecimal.valueOf(t.duracionDias == null ? 0 : t.duracionDias);
            suma = suma.add(t.avance.multiply(peso));
            pesos = pesos.add(peso);
        }
        if (pesos.signum() == 0) {
            BigDecimal total = ts.stream().map(t -> t.avance).reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.divide(BigDecimal.valueOf(ts.size()), 2, RoundingMode.HALF_UP);
        }
        return suma.divide(pesos, 2, RoundingMode.HALF_UP);
    }

}
