package cl.proyecto.gestion.planificacion.lineabase;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue A-14: fijar la línea base (copia el plan vigente a las columnas *_base y registra una versión) y comparar
 * base / plan / real. Los cambios posteriores (duraciones, dependencias, solicitudes aprobadas) modifican solo el
 * plan vigente: el recálculo nunca toca las columnas *_base.
 */
@Service
public class LineaBaseService {

    private final LineaBaseRepository lineasBase;
    private final TareaRepository tareas;
    private final ProyectoRepository proyectosRepo;
    private final ProyectoService proyectos;
    private final RecalculoService recalculo;
    private final CalendarioProyectos calendarios;
    private final ComparadorPlanes comparador = new ComparadorPlanes();

    public LineaBaseService(LineaBaseRepository lineasBase, TareaRepository tareas, ProyectoRepository proyectosRepo,
                            ProyectoService proyectos, RecalculoService recalculo, CalendarioProyectos calendarios) {
        this.lineasBase = lineasBase;
        this.tareas = tareas;
        this.proyectosRepo = proyectosRepo;
        this.proyectos = proyectos;
        this.recalculo = recalculo;
        this.calendarios = calendarios;
    }

    /** Fija una nueva versión de la línea base con el plan vigente (recalculado justo antes). */
    @Transactional
    public LineaBase fijar(Long proyectoId, String motivo) {
        Proyecto p = proyectos.obtenerEditable(proyectoId);
        recalculo.recalcular(proyectoId);
        List<Tarea> todas = tareas.findByProyectoId(proyectoId);
        if (todas.isEmpty() || todas.stream().anyMatch(t -> t.inicioPlan == null || t.finPlan == null)) {
            throw ApiException.conflicto("SIN_PLAN",
                "No se puede fijar la línea base: el proyecto necesita fecha de inicio y tareas con fechas calculadas.");
        }
        BigDecimal costoTotal = BigDecimal.ZERO;
        for (Tarea t : todas) {
            t.inicioBase = t.inicioPlan;
            t.finBase = t.finPlan;
            t.costoBase = t.costoPlan;
            if (t.parentId == null) {
                costoTotal = costoTotal.add(t.costoPlan);
            }
        }
        tareas.saveAll(todas);
        p.fechaFinBase = p.fechaFinPlan;
        proyectosRepo.save(p);

        int version = lineasBase.findFirstByProyectoIdOrderByVersionDesc(proyectoId).map(l -> l.version + 1).orElse(1);
        LineaBase lb = new LineaBase();
        lb.proyectoId = proyectoId;
        lb.version = version;
        lb.motivo = motivo == null || motivo.isBlank() ? "Línea base v" + version : motivo.trim();
        lb.fechaFin = p.fechaFinPlan;
        lb.costoTotal = costoTotal;
        return lineasBase.save(lb);
    }

    @Transactional(readOnly = true)
    public List<LineaBase> listar(Long proyectoId) {
        proyectos.obtener(proyectoId);
        return lineasBase.findByProyectoIdOrderByVersionDesc(proyectoId);
    }

    /** Compara base, plan y real de todo el proyecto. "hoy" nulo = fecha actual. */
    @Transactional(readOnly = true)
    public ComparacionPlan comparar(Long proyectoId, LocalDate hoy) {
        Proyecto p = proyectos.obtener(proyectoId);
        LineaBase ultima = lineasBase.findFirstByProyectoIdOrderByVersionDesc(proyectoId).orElse(null);
        return comparador.comparar(p, tareas.findByProyectoId(proyectoId), ultima,
            calendarios.de(p), hoy == null ? LocalDate.now() : hoy);
    }
}
