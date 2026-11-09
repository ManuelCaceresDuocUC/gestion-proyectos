package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.hito.Hito;
import cl.proyecto.gestion.hito.HitoRepository;
import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlerta;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaRepository;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoRepository;
import cl.proyecto.gestion.recurso.TipoRecurso;
import cl.proyecto.gestion.recurso.TipoRecursoRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma las variables de proyecto e hitos desde lo ya persistido. No evalúa fórmulas. */
@Service
public class VariablesProyectoService {

    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final HitoRepository hitos;
    private final AsignacionRepository asignaciones;
    private final RecursoRepository recursos;
    private final TipoRecursoRepository tipos;
    private final SobrecargaAlertaRepository alertas;
    private final CalendarioProyectos calendarios;

    public VariablesProyectoService(
            ProyectoRepository proyectos,
            TareaRepository tareas,
            HitoRepository hitos,
            AsignacionRepository asignaciones,
            RecursoRepository recursos,
            TipoRecursoRepository tipos,
            SobrecargaAlertaRepository alertas,
            CalendarioProyectos calendarios) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.hitos = hitos;
        this.asignaciones = asignaciones;
        this.recursos = recursos;
        this.tipos = tipos;
        this.alertas = alertas;
        this.calendarios = calendarios;
    }

    @Transactional(readOnly = true)
    public VariablesProyecto.Medicion deProyecto(Long proyectoId) {
        return deProyecto(proyectoId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public VariablesProyecto.Medicion deProyecto(Long proyectoId, LocalDate hoy) {
        if (proyectoId == null) {
            throw new IllegalArgumentException("El proyecto es obligatorio.");
        }
        Proyecto proyecto = proyectos.findById(proyectoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el proyecto " + proyectoId + "."));
        List<Tarea> edt = tareas.findByProyectoId(proyectoId);
        List<VariablesProyecto.Nodo> nodos = edt.stream()
                .map(t -> new VariablesProyecto.Nodo(
                        t.id, t.parentId, t.duracionDias, t.avance, t.costoPlan, t.inicioPlan, t.finPlan))
                .toList();
        List<VariablesProyecto.HitoMedido> logros = hitos.findByProyectoIdOrderByFechaPlanAscCodigoAsc(proyectoId)
                .stream()
                .map(this::hito)
                .toList();
        Conteos profesionales = profesionales(proyectoId, edt);
        return VariablesProyecto.calcular(new VariablesProyecto.Datos(
                proyecto.avance,
                proyecto.fechaFinBase,
                proyecto.fechaFinPlan,
                nodos,
                logros,
                profesionales.total(),
                profesionales.sobreasignados(),
                calendarios.de(proyecto),
                hoy,
                proyecto.costoReal));
    }

    private VariablesProyecto.HitoMedido hito(Hito hito) {
        return new VariablesProyecto.HitoMedido(
                hito.fechaPlan,
                hito.fechaReal,
                hito.estado == cl.proyecto.gestion.hito.EstadoHito.CUMPLIDO);
    }

    private Conteos profesionales(Long proyectoId, List<Tarea> edt) {
        TipoRecurso tipo = tipos.findByNombreIgnoreCase(VariablesProyecto.TIPO_PROFESIONAL).orElse(null);
        if (tipo == null) {
            return new Conteos(0, 0);
        }
        Set<Long> asignados = new HashSet<>();
        for (Tarea tarea : edt) {
            for (Asignacion asignacion : asignaciones.findByTareaIdOrderById(tarea.id)) {
                Recurso recurso = recursos.findById(asignacion.recursoId).orElse(null);
                if (recurso != null && tipo.id.equals(recurso.tipoRecursoId)) {
                    asignados.add(recurso.id);
                }
            }
        }
        Set<Long> sobre = new HashSet<>();
        for (SobrecargaAlerta alerta : alertas.findByProyectoIdOrderByFechaAscRecursoNombreAsc(proyectoId)) {
            if (asignados.contains(alerta.recursoId)) {
                sobre.add(alerta.recursoId);
            }
        }
        return new Conteos(asignados.size(), sobre.size());
    }

    private record Conteos(int total, int sobreasignados) {
    }
}
