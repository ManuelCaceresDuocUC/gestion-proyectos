package cl.proyecto.gestion.proyecto;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issue A-04: crear, editar, consultar y cerrar proyectos. */
@Service
public class ProyectoService {

    private final ProyectoRepository repo;
    private final RecalculoService recalculo;

    public ProyectoService(ProyectoRepository repo, RecalculoService recalculo) {
        this.repo = repo;
        this.recalculo = recalculo;
    }

    @Transactional(readOnly = true)
    public List<Proyecto> listar(EstadoProyecto estado, Long responsableId) {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "creadoEn")).stream()
            .filter(p -> estado == null || p.estado == estado)
            .filter(p -> responsableId == null || responsableId.equals(p.responsableId))
            .toList();
    }

    @Transactional(readOnly = true)
    public Proyecto obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe el proyecto " + id + "."));
    }

    @Transactional
    public Proyecto crear(ProyectoRequest r) {
        rechazarCierreDirecto(r);
        if (repo.existsByCodigo(r.codigo().trim())) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un proyecto con el código " + r.codigo().trim() + ".");
        }
        Proyecto p = new Proyecto();
        aplicar(p, r);
        return repo.save(p);
    }

    @Transactional
    public Proyecto actualizar(Long id, ProyectoRequest r) {
        Proyecto p = obtenerEditable(id);
        rechazarCierreDirecto(r);
        String nuevoCodigo = r.codigo().trim();
        if (!nuevoCodigo.equals(p.codigo) && repo.existsByCodigo(nuevoCodigo)) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un proyecto con el código " + nuevoCodigo + ".");
        }
        aplicar(p, r);
        repo.save(p);
        recalculo.recalcular(p.id); // la fecha de inicio puede haber cambiado
        return p;
    }

    /** Cierra el proyecto: queda en solo lectura. Si no tenía fecha de fin real, se usa la de hoy. */
    @Transactional
    public Proyecto cerrar(Long id) {
        Proyecto p = obtenerEditable(id);
        p.estado = EstadoProyecto.TERMINADO;
        p.cerradoEn = Instant.now();
        if (p.fechaFinReal == null) {
            p.fechaFinReal = LocalDate.now();
        }
        return repo.save(p);
    }

    /** Elimina un proyecto que aún no empieza (estado PLANIFICADO) junto con su EDT, recursos asignados e hitos. */
    @Transactional
    public void eliminar(Long id) {
        Proyecto p = obtener(id);
        if (p.estado != EstadoProyecto.PLANIFICADO) {
            throw ApiException.conflicto("PROYECTO_NO_ELIMINABLE",
                "Solo se pueden eliminar proyectos en estado PLANIFICADO. Este está " + p.estado + ".");
        }
        repo.delete(p);
        repo.flush();
    }

    /** Devuelve el proyecto o falla si no existe o ya está cerrado. Lo usan también la EDT y los demás módulos. */
    public Proyecto obtenerEditable(Long id) {
        Proyecto p = obtener(id);
        if (p.estado == EstadoProyecto.TERMINADO) {
            throw ApiException.conflicto("PROYECTO_CERRADO", "El proyecto " + p.codigo + " está cerrado y no admite cambios.");
        }
        return p;
    }

    private void rechazarCierreDirecto(ProyectoRequest r) {
        if (r.estado() == EstadoProyecto.TERMINADO) {
            throw ApiException.invalido("USAR_CERRAR", "Para cerrar un proyecto usa POST /api/proyectos/{id}/cerrar.");
        }
    }

    private void aplicar(Proyecto p, ProyectoRequest r) {
        p.codigo = r.codigo().trim();
        p.nombre = r.nombre().trim();
        p.descripcion = r.descripcion();
        if (r.estado() != null) {
            p.estado = r.estado();
        }
        p.responsableId = r.responsableId();
        p.presupuesto = r.presupuesto();
        if (r.moneda() != null) {
            p.moneda = r.moneda().toUpperCase();
        }
        if (r.diasLaborales() != null) {
            p.diasLaborales = r.diasLaborales();
        }
        p.fechaInicio = r.fechaInicio();
    }
}
