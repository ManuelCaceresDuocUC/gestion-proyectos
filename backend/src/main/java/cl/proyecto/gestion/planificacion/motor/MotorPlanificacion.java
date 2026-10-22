package cl.proyecto.gestion.planificacion.motor;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de planificación: dominio puro (no usa base de datos ni otros módulos).
 * Issues: A-07 (FS y SS con desfases), A-08 (FF y SF), A-09 (ciclos).
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
     * Calcula inicio y fin de cada tarea respetando los 4 tipos de dependencia y sus desfases.
     * TODO A-07 / A-08: implementar (orden topológico + paso hacia adelante). Documentar en un ADR
     * la semántica exacta de SF y de los desfases negativos.
     */
    public Map<Long, FechasCalculadas> calcular(LocalDate inicioProyecto, Collection<TareaPlan> tareas,
                                                Collection<DependenciaPlan> deps, Calendario calendario) {
        validarSinCiclos(tareas.stream().map(TareaPlan::id).toList(), deps);
        throw new UnsupportedOperationException("Pendiente: issues A-07 y A-08");
    }
}
