package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Calcula carga con cantidad como horas totales del profesional. horasSemana y semanas
 * son datos complementarios y no participan en B-09. El paso reconstruye las alertas
 * para ser idempotente.
 */
@Component
@Order(OrdenRecalculo.SOBREASIGNACION)
public class PasoSobreasignacion implements PasoRecalculo {

    private static final Logger log = LoggerFactory.getLogger(PasoSobreasignacion.class);
    private static final int ESCALA_CARGA = 4;

    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final AsignacionRepository asignaciones;
    private final RecursoRepository recursos;
    private final CalendarioProyectos calendarios;
    private final SobrecargaAlertaRepository alertas;
    private final SobrecargaAlertaTareaRepository alertaTareas;

    public PasoSobreasignacion(
        ProyectoRepository proyectos,
        TareaRepository tareas,
        AsignacionRepository asignaciones,
        RecursoRepository recursos,
        CalendarioProyectos calendarios,
        SobrecargaAlertaRepository alertas,
        SobrecargaAlertaTareaRepository alertaTareas) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.asignaciones = asignaciones;
        this.recursos = recursos;
        this.calendarios = calendarios;
        this.alertas = alertas;
        this.alertaTareas = alertaTareas;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }

        Map<Long, Tarea> porId = new HashMap<>();
        Set<Long> conHijos = new HashSet<>();
        tareas.findByProyectoId(proyectoId).forEach(tarea -> {
            porId.put(tarea.id, tarea);
            if (tarea.parentId != null) {
                conHijos.add(tarea.parentId);
            }
        });

        Map<CargaKey, Carga> cargas = new HashMap<>();
        Calendario calendario = calendarios.de(proyecto);
        for (Tarea tarea : porId.values()) {
            if (conHijos.contains(tarea.id) || tarea.duracionDias == null || tarea.duracionDias == 0) {
                continue;
            }
            if (tarea.inicioPlan == null || tarea.finPlan == null) {
                log.debug("Sobreasignación: tarea {} sin fechas plan, no se calcula carga", tarea.id);
                continue;
            }
            List<LocalDate> dias = diasHabiles(calendario, tarea.inicioPlan, tarea.finPlan);
            if (dias.isEmpty()) {
                log.debug("Sobreasignación: tarea {} sin días hábiles en el intervalo plan, no se calcula carga", tarea.id);
                continue;
            }
            for (Asignacion asignacion : asignaciones.findByTareaIdOrderById(tarea.id)) {
                Recurso recurso = recursos.findById(asignacion.recursoId).orElse(null);
                if (recurso == null || recurso.capacidadHorasDia == null) {
                    continue;
                }
                BigDecimal diaria = asignacion.cantidad.divide(
                    BigDecimal.valueOf(dias.size()), ESCALA_CARGA, RoundingMode.HALF_UP);
                for (LocalDate dia : dias) {
                    Carga carga = cargas.computeIfAbsent(
                        new CargaKey(recurso.id, dia),
                        k -> new Carga(recurso, dia));
                    carga.horas = carga.horas.add(diaria);
                    carga.tareaIds.add(tarea.id);
                }
            }
        }

        alertas.deleteByProyectoId(proyectoId);
        alertas.flush();
        for (Carga carga : cargas.values()) {
            if (carga.horas.compareTo(carga.recurso.capacidadHorasDia) <= 0) {
                continue;
            }
            SobrecargaAlerta alerta = new SobrecargaAlerta();
            alerta.proyectoId = proyectoId;
            alerta.recursoId = carga.recurso.id;
            alerta.recursoNombre = carga.recurso.nombre;
            alerta.fecha = carga.fecha;
            alerta.cargaHoras = carga.horas;
            alerta.capacidadHorasDia = carga.recurso.capacidadHorasDia;
            alerta.excesoHoras = carga.horas.subtract(carga.recurso.capacidadHorasDia);
            alertas.save(alerta);
            alertas.flush();
            for (Long tareaId : carga.tareaIds) {
                Tarea tarea = porId.get(tareaId);
                SobrecargaAlertaTarea detalle = new SobrecargaAlertaTarea();
                detalle.alertaId = alerta.id;
                detalle.tareaId = tarea.id;
                detalle.tareaCodigo = tarea.codigo;
                detalle.tareaNombre = tarea.nombre;
                alertaTareas.save(detalle);
            }
        }
    }

    private List<LocalDate> diasHabiles(Calendario calendario, LocalDate inicio, LocalDate fin) {
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (calendario.esLaborable(dia)) {
                dias.add(dia);
            }
        }
        return dias;
    }

    private record CargaKey(Long recursoId, LocalDate fecha) {}

    private static class Carga {
        private final Recurso recurso;
        private final LocalDate fecha;
        private BigDecimal horas = BigDecimal.ZERO.setScale(ESCALA_CARGA);
        private final Set<Long> tareaIds = new HashSet<>();

        private Carga(Recurso recurso, LocalDate fecha) {
            this.recurso = recurso;
            this.fecha = fecha;
        }
    }
}
