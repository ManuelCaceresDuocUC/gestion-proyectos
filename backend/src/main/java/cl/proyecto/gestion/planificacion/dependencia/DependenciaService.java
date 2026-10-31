package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.motor.DependenciaPlan;
import cl.proyecto.gestion.planificacion.motor.MotorPlanificacion;
import cl.proyecto.gestion.planificacion.recalculo.InstantaneaFechas;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.planificacion.recalculo.ResultadoRecalculo;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues A-09 y A-13: alta, edición y baja de dependencias. Cada operación valida, guarda, recalcula el proyecto
 * y responde con las fechas que cambiaron. Un ciclo, una dependencia consigo misma, una duplicada o una que toque
 * una etapa/actividad con hijos se rechazan con 4xx y no se guarda nada.
 */
@Service
public class DependenciaService {

    private final DependenciaRepository dependencias;
    private final TareaRepository tareas;
    private final ProyectoService proyectos;
    private final RecalculoService recalculo;
    private final MotorPlanificacion motor = new MotorPlanificacion();

    public DependenciaService(DependenciaRepository dependencias, TareaRepository tareas,
                              ProyectoService proyectos, RecalculoService recalculo) {
        this.dependencias = dependencias;
        this.tareas = tareas;
        this.proyectos = proyectos;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<DependenciaResponse> listar(Long proyectoId) {
        proyectos.obtener(proyectoId);
        return dependencias.findByProyecto(proyectoId).stream().map(DependenciaResponse::de).toList();
    }

    @Transactional
    public ResultadoDependencia crear(Long proyectoId, DependenciaRequest r) {
        proyectos.obtenerEditable(proyectoId);
        Tarea pred = tareaDelProyecto(r.predecesoraId(), proyectoId);
        Tarea suc = tareaDelProyecto(r.sucesoraId(), proyectoId);
        if (pred.id.equals(suc.id)) {
            throw ApiException.invalido("DEPENDENCIA_A_SI_MISMA",
                "Una tarea no puede depender de sí misma (" + pred.codigo + " " + pred.nombre + ").");
        }
        exigirHoja(pred);
        exigirHoja(suc);

        List<Dependencia> existentes = dependencias.findByProyecto(proyectoId);
        if (existentes.stream().anyMatch(d -> d.predecesoraId.equals(pred.id) && d.sucesoraId.equals(suc.id))) {
            throw ApiException.conflicto("DEPENDENCIA_DUPLICADA",
                "Ya existe una dependencia de " + pred.codigo + " a " + suc.codigo + ". Edítela en vez de crear otra.");
        }
        rechazarCiclo(pred, suc, existentes, proyectoId);

        InstantaneaFechas antes = InstantaneaFechas.tomar(tareas.findByProyectoId(proyectoId));
        Dependencia d = new Dependencia();
        d.predecesoraId = pred.id;
        d.sucesoraId = suc.id;
        d.tipo = r.tipo();
        d.desfaseDias = r.desfaseDias() == null ? 0 : r.desfaseDias();
        dependencias.save(d);
        recalculo.recalcular(proyectoId);
        return new ResultadoDependencia(DependenciaResponse.de(d), antes.cambiosHasta(tareas.findByProyectoId(proyectoId)));
    }

    @Transactional
    public ResultadoDependencia actualizar(Long id, DependenciaEdicion r) {
        Dependencia d = obtener(id);
        Long proyectoId = tareas.findById(d.predecesoraId).map(t -> t.proyectoId).orElseThrow();
        proyectos.obtenerEditable(proyectoId);
        InstantaneaFechas antes = InstantaneaFechas.tomar(tareas.findByProyectoId(proyectoId));
        d.tipo = r.tipo();
        d.desfaseDias = r.desfaseDias() == null ? 0 : r.desfaseDias();
        dependencias.save(d);
        recalculo.recalcular(proyectoId);
        return new ResultadoDependencia(DependenciaResponse.de(d), antes.cambiosHasta(tareas.findByProyectoId(proyectoId)));
    }

    @Transactional
    public ResultadoRecalculo eliminar(Long id) {
        Dependencia d = obtener(id);
        Long proyectoId = tareas.findById(d.predecesoraId).map(t -> t.proyectoId).orElseThrow();
        proyectos.obtenerEditable(proyectoId);
        InstantaneaFechas antes = InstantaneaFechas.tomar(tareas.findByProyectoId(proyectoId));
        dependencias.delete(d);
        dependencias.flush();
        recalculo.recalcular(proyectoId);
        return new ResultadoRecalculo(antes.cambiosHasta(tareas.findByProyectoId(proyectoId)));
    }

    // ---------------------------------------------------------------- utilidades

    private Dependencia obtener(Long id) {
        return dependencias.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la dependencia " + id + "."));
    }

    private Tarea tareaDelProyecto(Long id, Long proyectoId) {
        Tarea t = tareas.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la tarea " + id + "."));
        if (!t.proyectoId.equals(proyectoId)) {
            throw ApiException.invalido("TAREA_DE_OTRO_PROYECTO",
                "La tarea " + t.codigo + " " + t.nombre + " pertenece a otro proyecto.");
        }
        return t;
    }

    private void exigirHoja(Tarea t) {
        if (tareas.existsByParentId(t.id)) {
            throw ApiException.conflicto("DEPENDENCIA_EN_PADRE",
                "La dependencia debe ser entre tareas sin subtareas. " + t.codigo + " " + t.nombre
                    + " tiene subtareas: use alguna de ellas.");
        }
    }

    /** Hay ciclo si ya existe un camino sucesora -> ... -> predecesora. Explica el ciclo con los códigos EDT. */
    private void rechazarCiclo(Tarea pred, Tarea suc, List<Dependencia> existentes, Long proyectoId) {
        List<DependenciaPlan> planes = existentes.stream()
            .map(d -> new DependenciaPlan(d.predecesoraId, d.sucesoraId, d.tipo, d.desfaseDias)).toList();
        List<Long> camino = motor.caminoEntre(suc.id, pred.id, planes);
        if (camino.isEmpty()) {
            return;
        }
        Map<Long, Tarea> porId = new HashMap<>();
        tareas.findByProyectoId(proyectoId).forEach(t -> porId.put(t.id, t));
        String ruta = pred.codigo + " " + pred.nombre + " → " + camino.stream()
            .map(porId::get).map(t -> t.codigo + " " + t.nombre).collect(Collectors.joining(" → "));
        throw ApiException.conflicto("CICLO_EN_DEPENDENCIAS",
            "La dependencia " + pred.codigo + " → " + suc.codigo + " crea un ciclo: " + ruta + ".");
    }
}
