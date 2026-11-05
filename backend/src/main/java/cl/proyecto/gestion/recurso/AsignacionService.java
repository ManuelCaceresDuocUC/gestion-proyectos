package cl.proyecto.gestion.recurso;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsignacionService {

    private final AsignacionRepository repo;
    private final TareaRepository tareas;
    private final RecursoService recursos;
    private final ProyectoService proyectos;
    private final RecalculoService recalculo;

    public AsignacionService(
        AsignacionRepository repo,
        TareaRepository tareas,
        RecursoService recursos,
        ProyectoService proyectos,
        RecalculoService recalculo) {
        this.repo = repo;
        this.tareas = tareas;
        this.recursos = recursos;
        this.proyectos = proyectos;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<Asignacion> listarPorTarea(Long tareaId) {
        obtenerTarea(tareaId);
        return repo.findByTareaIdOrderById(tareaId);
    }

    @Transactional(readOnly = true)
    public Asignacion obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la asignación " + id + "."));
    }

    @Transactional
    public Asignacion crear(AsignacionRequest request) {
        Tarea tarea = validarTareaHoja(request.tareaId());
        Recurso recurso = validarRecursoActivo(request.recursoId());

        if (repo.existsByTareaIdAndRecursoId(request.tareaId(), request.recursoId())) {
            throw ApiException.conflicto(
                "ASIGNACION_DUPLICADA",
                "La tarea ya tiene asignado ese recurso.");
        }

        Asignacion asignacion = new Asignacion();
        aplicar(asignacion, request, recurso);
        repo.save(asignacion);
        recalculo.recalcular(tarea.proyectoId);
        return asignacion;
    }

    @Transactional
    public Asignacion actualizar(Long id, AsignacionRequest request) {
        Asignacion asignacion = obtener(id);
        Tarea tareaActual = obtenerTarea(asignacion.tareaId);
        proyectos.obtenerEditable(tareaActual.proyectoId);

        Tarea nuevaTarea = validarTareaHoja(request.tareaId());
        Recurso recurso = validarRecursoActivo(request.recursoId());

        if (!nuevaTarea.proyectoId.equals(tareaActual.proyectoId)) {
            throw ApiException.invalido(
                "TAREA_DE_OTRO_PROYECTO",
                "La nueva tarea pertenece a otro proyecto.");
        }

        repo.findByTareaIdAndRecursoId(request.tareaId(), request.recursoId())
            .filter(otra -> !otra.id.equals(id))
            .ifPresent(otra -> {
                throw ApiException.conflicto(
                    "ASIGNACION_DUPLICADA",
                    "La tarea ya tiene asignado ese recurso.");
            });

        aplicar(asignacion, request, recurso);
        repo.save(asignacion);
        recalculo.recalcular(tareaActual.proyectoId);
        return asignacion;
    }

    @Transactional
    public void eliminar(Long id) {
        Asignacion asignacion = obtener(id);
        Tarea tarea = obtenerTarea(asignacion.tareaId);
        proyectos.obtenerEditable(tarea.proyectoId);

        repo.delete(asignacion);
        repo.flush();
        recalculo.recalcular(tarea.proyectoId);
    }

    private Tarea validarTareaHoja(Long tareaId) {
        Tarea tarea = obtenerTarea(tareaId);
        proyectos.obtenerEditable(tarea.proyectoId);

        if (tareas.existsByParentId(tareaId)) {
            throw ApiException.conflicto(
                "TAREA_NO_HOJA",
                "Las asignaciones solo pueden asociarse a tareas sin subtareas.");
        }

        return tarea;
    }

    private Tarea obtenerTarea(Long tareaId) {
        return tareas.findById(tareaId)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la tarea " + tareaId + "."));
    }

    private Recurso validarRecursoActivo(Long recursoId) {
        Recurso recurso = recursos.obtener(recursoId);

        if (!recurso.activo) {
            throw ApiException.conflicto(
                "RECURSO_INACTIVO",
                "No se puede asignar un recurso inactivo.");
        }

        return recurso;
    }

    private void aplicar(
        Asignacion asignacion,
        AsignacionRequest request,
        Recurso recurso) {

        asignacion.tareaId = request.tareaId();
        asignacion.recursoId = request.recursoId();
        asignacion.cantidad = request.cantidad();
        asignacion.tarifa = request.tarifa() != null
            ? request.tarifa()
            : recurso.tarifa;
        asignacion.horasSemana = request.horasSemana();
        asignacion.semanas = request.semanas();

        if (asignacion.cantidadReal == null) {
            asignacion.cantidadReal = BigDecimal.ZERO;
        }
        if (asignacion.costoPlan == null) {
            asignacion.costoPlan = BigDecimal.ZERO;
        }
        if (asignacion.costoReal == null) {
            asignacion.costoReal = BigDecimal.ZERO;
        }
    }
}