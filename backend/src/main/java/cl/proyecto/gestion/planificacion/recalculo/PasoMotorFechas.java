package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.dependencia.Dependencia;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaRepository;
import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.planificacion.motor.CicloDetectadoException;
import cl.proyecto.gestion.planificacion.motor.DependenciaPlan;
import cl.proyecto.gestion.planificacion.motor.FechasCalculadas;
import cl.proyecto.gestion.planificacion.motor.MotorPlanificacion;
import cl.proyecto.gestion.planificacion.motor.TareaPlan;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Paso MOTOR_FECHAS (A-12): lee las tareas hoja y sus dependencias, llama al motor y guarda inicio/fin plan.
 * Sin fecha de inicio del proyecto no hay nada que planificar, así que no hace nada.
 * Las dependencias que tocan a una etapa o actividad con hijos se ignoran (las fechas de los padres se derivan
 * de sus hijos); A-09 las rechazará al crearlas.
 */
@Component
@Order(OrdenRecalculo.MOTOR_FECHAS)
public class PasoMotorFechas implements PasoRecalculo {
    private static final Logger log = LoggerFactory.getLogger(PasoMotorFechas.class);

    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final DependenciaRepository dependencias;
    private final MotorPlanificacion motor = new MotorPlanificacion();

    public PasoMotorFechas(ProyectoRepository proyectos, TareaRepository tareas, DependenciaRepository dependencias) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.dependencias = dependencias;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId).orElse(null);
        if (proyecto == null || proyecto.fechaInicio == null) {
            log.debug("Proyecto {} sin fecha de inicio: no se calculan fechas", proyectoId);
            return;
        }
        List<Tarea> todas = tareas.findByProyectoId(proyectoId);
        Set<Long> conHijos = new HashSet<>();
        todas.forEach(t -> { if (t.parentId != null) conHijos.add(t.parentId); });
        List<Tarea> hojas = todas.stream().filter(t -> !conHijos.contains(t.id)).toList();
        Set<Long> idsHojas = new HashSet<>();
        hojas.forEach(t -> idsHojas.add(t.id));

        List<TareaPlan> planes = hojas.stream()
            .map(t -> new TareaPlan(t.id, t.duracionDias == null ? 1 : t.duracionDias, null)).toList();
        List<DependenciaPlan> deps = dependencias.findByProyecto(proyectoId).stream()
            .filter(d -> idsHojas.contains(d.predecesoraId) && idsHojas.contains(d.sucesoraId))
            .map(d -> new DependenciaPlan(d.predecesoraId, d.sucesoraId, d.tipo, d.desfaseDias)).toList();

        Map<Long, FechasCalculadas> fechas;
        try {
            fechas = motor.calcular(proyecto.fechaInicio, planes, deps, new Calendario(Set.of()));
        } catch (CicloDetectadoException e) {
            throw ApiException.conflicto("CICLO_EN_DEPENDENCIAS", e.getMessage());
        }
        for (Tarea t : hojas) {
            FechasCalculadas f = fechas.get(t.id);
            t.inicioPlan = f.inicio();
            t.finPlan = f.fin();
        }
        tareas.saveAll(hojas);
        tareas.flush();
    }
}
