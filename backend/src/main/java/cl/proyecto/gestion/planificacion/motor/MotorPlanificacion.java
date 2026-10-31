package cl.proyecto.gestion.planificacion.motor;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de planificación: dominio puro (no usa base de datos ni otros módulos).
 * Issues: A-07 (FS y SS con desfases), A-08 (FF y SF), A-09 (ciclos). Semántica en docs/gestion/decisiones/ADR-002-motor-fechas.md.
 */
public class MotorPlanificacion {

    /** Lanza CicloDetectadoException si las dependencias forman un ciclo (algoritmo de Kahn). */
    public void validarSinCiclos(Collection<Long> tareaIds, Collection<DependenciaPlan> deps) {
        Map<Long, Integer> gradoEntrada = new HashMap<>();
        Map<Long, List<Long>> salientes = new HashMap<>();
        for (Long id : tareaIds) {
            gradoEntrada.put(id, 0);
            salientes.put(id, new java.util.ArrayList<>());
        }
        for (DependenciaPlan d : deps) {
            if (!salientes.containsKey(d.predecesoraId()) || !salientes.containsKey(d.sucesoraId())) {
                throw new IllegalArgumentException("La dependencia " + d.predecesoraId() + " -> " + d.sucesoraId()
                    + " referencia una tarea que no está en la planificación.");
            }
            salientes.get(d.predecesoraId()).add(d.sucesoraId());
            gradoEntrada.merge(d.sucesoraId(), 1, Integer::sum);
        }
        ArrayDeque<Long> cola = new ArrayDeque<>();
        gradoEntrada.forEach((id, g) -> { if (g == 0) cola.add(id); });
        int visitadas = 0;
        while (!cola.isEmpty()) {
            Long id = cola.poll();
            visitadas++;
            for (Long s : salientes.get(id)) {
                if (gradoEntrada.merge(s, -1, Integer::sum) == 0) cola.add(s);
            }
        }
        if (visitadas != tareaIds.size()) {
            throw new CicloDetectadoException("Las dependencias forman un ciclo; no se puede planificar.");
        }
    }

    /**
     * Camino más corto de tareas desde "desde" hasta "hasta" siguiendo las dependencias (predecesora -> sucesora),
     * con ambos extremos incluidos; lista vacía si no hay camino. Sirve para explicar un ciclo al crear una dependencia.
     */
    public List<Long> caminoEntre(long desde, long hasta, Collection<DependenciaPlan> deps) {
        Map<Long, List<Long>> salientes = new HashMap<>();
        for (DependenciaPlan d : deps) {
            salientes.computeIfAbsent(d.predecesoraId(), k -> new ArrayList<>()).add(d.sucesoraId());
        }
        Map<Long, Long> anterior = new HashMap<>();
        ArrayDeque<Long> cola = new ArrayDeque<>();
        cola.add(desde);
        anterior.put(desde, desde);
        while (!cola.isEmpty()) {
            Long actual = cola.poll();
            if (actual == hasta) {
                ArrayList<Long> camino = new ArrayList<>();
                for (Long n = actual; ; n = anterior.get(n)) {
                    camino.add(0, n);
                    if (n == desde) break;
                }
                return camino;
            }
            for (Long s : salientes.getOrDefault(actual, List.of())) {
                if (!anterior.containsKey(s)) {
                    anterior.put(s, actual);
                    cola.add(s);
                }
            }
        }
        return List.of();
    }

    /**
     * Calcula inicio y fin de cada tarea respetando los 4 tipos de dependencia y sus desfases (ADR-002).
     * Paso hacia adelante en orden topológico: cada tarea empieza lo más temprano que permiten el inicio del
     * proyecto, su inicio mínimo y todas sus predecesoras (gana la más restrictiva).
     * Duraciones en días hábiles; el día de término es inclusivo: fin = inicio + (duración - 1) hábiles.
     * Una duración 0 es un hito: inicio = fin.
     */
    public Map<Long, FechasCalculadas> calcular(LocalDate inicioProyecto, Collection<TareaPlan> tareas,
                                                Collection<DependenciaPlan> deps, Calendario calendario) {
        List<Long> ids = tareas.stream().map(TareaPlan::id).toList();
        validarSinCiclos(ids, deps);

        Map<Long, TareaPlan> porId = new HashMap<>();
        for (TareaPlan t : tareas) porId.put(t.id(), t);
        Map<Long, List<DependenciaPlan>> entrantes = new HashMap<>();
        Map<Long, List<Long>> salientes = new HashMap<>();
        Map<Long, Integer> grado = new HashMap<>();
        for (Long id : ids) {
            entrantes.put(id, new ArrayList<>());
            salientes.put(id, new ArrayList<>());
            grado.put(id, 0);
        }
        for (DependenciaPlan d : deps) {
            entrantes.get(d.sucesoraId()).add(d);
            salientes.get(d.predecesoraId()).add(d.sucesoraId());
            grado.merge(d.sucesoraId(), 1, Integer::sum);
        }

        LocalDate base = diaHabil(inicioProyecto, calendario);
        Map<Long, FechasCalculadas> resultado = new LinkedHashMap<>();
        ArrayDeque<Long> cola = new ArrayDeque<>();
        ids.stream().filter(id -> grado.get(id) == 0).forEach(cola::add);
        while (!cola.isEmpty()) {
            Long id = cola.poll();
            TareaPlan t = porId.get(id);
            LocalDate inicio = base;
            if (t.inicioMinimo() != null) inicio = max(inicio, diaHabil(t.inicioMinimo(), calendario));
            for (DependenciaPlan d : entrantes.get(id)) {
                inicio = max(inicio, inicioMinimoPor(d, resultado.get(d.predecesoraId()), t.duracionDias(), calendario));
            }
            resultado.put(id, new FechasCalculadas(inicio, finDesde(inicio, t.duracionDias(), calendario)));
            for (Long s : salientes.get(id)) {
                if (grado.merge(s, -1, Integer::sum) == 0) cola.add(s);
            }
        }
        return resultado;
    }

    /** Inicio más temprano que impone una dependencia a la sucesora, según su tipo y desfase. */
    private LocalDate inicioMinimoPor(DependenciaPlan d, FechasCalculadas pred, int duracion, Calendario cal) {
        return switch (d.tipo()) {
            case FS -> cal.sumarDiasHabiles(pred.fin(), 1 + d.desfaseDias());
            case SS -> cal.sumarDiasHabiles(pred.inicio(), d.desfaseDias());
            case FF -> inicioDesdeFin(cal.sumarDiasHabiles(pred.fin(), d.desfaseDias()), duracion, cal);
            case SF -> inicioDesdeFin(cal.sumarDiasHabiles(pred.inicio(), d.desfaseDias()), duracion, cal);
        };
    }

    private static LocalDate finDesde(LocalDate inicio, int duracion, Calendario cal) {
        return duracion <= 1 ? inicio : cal.sumarDiasHabiles(inicio, duracion - 1);
    }

    private static LocalDate inicioDesdeFin(LocalDate fin, int duracion, Calendario cal) {
        return duracion <= 1 ? fin : cal.sumarDiasHabiles(fin, -(duracion - 1));
    }

    /** Primer día hábil igual o posterior a la fecha dada. */
    private static LocalDate diaHabil(LocalDate d, Calendario cal) {
        LocalDate r = d;
        while (!cal.esLaborable(r)) r = r.plusDays(1);
        return r;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
