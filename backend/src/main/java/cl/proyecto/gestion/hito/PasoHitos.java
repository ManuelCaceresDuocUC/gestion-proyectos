package cl.proyecto.gestion.hito;

import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.planificacion.recalculo.OrdenRecalculo;
import cl.proyecto.gestion.planificacion.recalculo.PasoRecalculo;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Paso 50. Si la fecha prevista queda antes del fin planificado de una actividad asociada,
 * la mueve al último día hábil de la semana de ese término. Un CUMPLIDO que deja de cumplir
 * B-11 vuelve a PENDIENTE. No promueve estados ni toca la línea base ni la fecha real.
 */
@Component
@Order(OrdenRecalculo.HITOS)
public class PasoHitos implements PasoRecalculo {

    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final HitoRepository hitos;
    private final HitoTareaRepository vinculos;
    private final CalendarioProyectos calendarios;

    public PasoHitos(ProyectoRepository proyectos, TareaRepository tareas, HitoRepository hitos,
                     HitoTareaRepository vinculos, CalendarioProyectos calendarios) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.hitos = hitos;
        this.vinculos = vinculos;
        this.calendarios = calendarios;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }
        List<Hito> delProyecto = hitos.findByProyectoIdOrderByFechaPlanAscCodigoAsc(proyectoId);
        if (delProyecto.isEmpty()) {
            return;
        }

        List<Long> ids = delProyecto.stream().map(h -> h.id).toList();
        Map<Long, List<Long>> porHito = new HashMap<>();
        for (HitoTarea v : vinculos.findByHitoIdIn(ids)) {
            porHito.computeIfAbsent(v.hitoId, k -> new ArrayList<>()).add(v.tareaId);
        }
        Map<Long, Tarea> porTarea = new HashMap<>();
        List<Long> tareaIds = porHito.values().stream().flatMap(List::stream).distinct().toList();
        if (!tareaIds.isEmpty()) {
            for (Tarea t : tareas.findAllById(tareaIds)) {
                porTarea.put(t.id, t);
            }
        }
        Calendario calendario = proyecto.fechaInicio == null ? null : calendarios.de(proyecto);

        for (Hito hito : delProyecto) {
            List<Long> idsDeHito = porHito.getOrDefault(hito.id, List.of());
            List<Tarea> asociadas = idsDeHito.stream()
                .map(porTarea::get)
                .filter(t -> t != null)
                .toList();
            adelantarFechaSiQuedaAntesDelTermino(hito, proyecto, calendario, asociadas);
            if (hito.estado == EstadoHito.CUMPLIDO && !sigueCumplido(hito, idsDeHito, porTarea)) {
                hito.estado = EstadoHito.PENDIENTE;
            }
        }
        hitos.saveAll(delProyecto);
    }

    /**
     * Misma regla que el seed: el viernes de la semana del término más tardío.
     * Si la fecha prevista ya es posterior, se conserva.
     */
    private void adelantarFechaSiQuedaAntesDelTermino(Hito hito, Proyecto proyecto, Calendario calendario,
                                                      List<Tarea> asociadas) {
        if (calendario == null || proyecto.fechaInicio == null) {
            return;
        }
        LocalDate finMasTarde = null;
        for (Tarea t : asociadas) {
            if (t.finPlan != null && (finMasTarde == null || t.finPlan.isAfter(finMasTarde))) {
                finMasTarde = t.finPlan;
            }
        }
        if (finMasTarde == null || (hito.fechaPlan != null && !hito.fechaPlan.isBefore(finMasTarde))) {
            return;
        }
        int semana = SemanaHito.semanaDe(proyecto.fechaInicio, calendario, finMasTarde);
        hito.fechaPlan = SemanaHito.fechaDeSemana(proyecto.fechaInicio, calendario, semana);
    }

    /**
     * B-11: evidencia, fecha real y cada actividad asociada terminada, con la fecha real
     * no anterior a ese término. Si no hay vínculos, o alguno no resuelve a una tarea, no sigue cumplido.
     */
    private boolean sigueCumplido(Hito hito, List<Long> tareaIds, Map<Long, Tarea> porTarea) {
        if (hito.evidencia == null || hito.evidencia.isBlank() || hito.fechaReal == null || tareaIds.isEmpty()) {
            return false;
        }
        for (Long tareaId : tareaIds) {
            Tarea t = porTarea.get(tareaId);
            if (t == null || t.finReal == null || hito.fechaReal.isBefore(t.finReal)) {
                return false;
            }
        }
        return true;
    }
}
