package cl.proyecto.gestion.planificacion.calendario;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.proyecto.EstadoProyecto;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoLaborableService {

    private final NoLaborableRepository repo;
    private final ProyectoService proyectos;
    private final RecalculoService recalculo;

    public NoLaborableService(
        NoLaborableRepository repo,
        ProyectoService proyectos,
        RecalculoService recalculo) {
        this.repo = repo;
        this.proyectos = proyectos;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<NoLaborable> listar(Long proyectoId) {
        if (proyectoId != null) {
            proyectos.obtener(proyectoId);
            return repo.findByProyectoIdOrProyectoIdIsNullOrderByFecha(proyectoId);
        }
        return repo.findByProyectoIdIsNullOrderByFecha();
    }

    @Transactional(readOnly = true)
    public NoLaborable obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe el día no laborable " + id + "."));
    }

    @Transactional
    public NoLaborable crear(NoLaborableRequest request) {
        validarProyecto(request.proyectoId());
        validarDuplicado(request.proyectoId(), request.fecha(), null);
        NoLaborable noLaborable = new NoLaborable();
        aplicar(noLaborable, request);
        NoLaborable guardado = guardar(noLaborable);
        if (guardado.proyectoId == null) {
            recalcularTodosLosProyectosEditables();
        } else {
            recalculo.recalcular(guardado.proyectoId);
        }
        return guardado;
    }

    @Transactional
    public NoLaborable actualizar(Long id, NoLaborableRequest request) {
        NoLaborable noLaborable = obtener(id);
        Long proyectoAnterior = noLaborable.proyectoId;
        validarProyecto(proyectoAnterior);
        validarProyecto(request.proyectoId());
        validarDuplicado(request.proyectoId(), request.fecha(), id);
        aplicar(noLaborable, request);
        NoLaborable actualizado = guardar(noLaborable);
        if (proyectoAnterior == null || actualizado.proyectoId == null) {
            recalcularTodosLosProyectosEditables();
        } else {
            recalcularAfectados(proyectoAnterior, actualizado.proyectoId);
        }
        return actualizado;
    }

    @Transactional
    public void eliminar(Long id) {
        NoLaborable noLaborable = obtener(id);
        validarProyecto(noLaborable.proyectoId);
        repo.delete(noLaborable);
        repo.flush();
        if (noLaborable.proyectoId == null) {
            recalcularTodosLosProyectosEditables();
        } else {
            recalculo.recalcular(noLaborable.proyectoId);
        }
    }

    private void validarProyecto(Long proyectoId) {
        if (proyectoId != null) {
            proyectos.obtenerEditable(proyectoId);
        }
    }

    private void aplicar(NoLaborable noLaborable, NoLaborableRequest request) {
        noLaborable.proyectoId = request.proyectoId();
        noLaborable.fecha = request.fecha();
        noLaborable.descripcion = request.descripcion();
    }

    private void validarDuplicado(Long proyectoId, LocalDate fecha, Long id) {
        boolean existe = id == null
            ? proyectoId == null
                ? repo.existsByProyectoIdIsNullAndFecha(fecha)
                : repo.existsByProyectoIdAndFecha(proyectoId, fecha)
            : proyectoId == null
                ? repo.existsByProyectoIdIsNullAndFechaAndIdNot(fecha, id)
                : repo.existsByProyectoIdAndFechaAndIdNot(proyectoId, fecha, id);
        if (existe) {
            throw ApiException.conflicto(
                "DIA_NO_LABORABLE_DUPLICADO",
                "Ya existe un día no laborable para ese proyecto y fecha.");
        }
    }

    private NoLaborable guardar(NoLaborable noLaborable) {
        try {
            return repo.saveAndFlush(noLaborable);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflicto(
                "DIA_NO_LABORABLE_DUPLICADO",
                "Ya existe un día no laborable para ese proyecto y fecha.");
        }
    }

    private void recalcularAfectados(Long proyectoAnterior, Long proyectoNuevo) {
        if (proyectoAnterior != null) {
            recalculo.recalcular(proyectoAnterior);
        }
        if (proyectoNuevo != null && !proyectoNuevo.equals(proyectoAnterior)) {
            recalculo.recalcular(proyectoNuevo);
        }
    }

    private void recalcularTodosLosProyectosEditables() {
        proyectos.listar(null, null).stream()
            .filter(proyecto -> proyecto.estado != EstadoProyecto.TERMINADO)
            .forEach(proyecto -> recalculo.recalcular(proyecto.id));
    }
}
