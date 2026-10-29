package cl.proyecto.gestion.proyecto;

import cl.proyecto.gestion.common.ApiException;
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

    public ProyectoService(ProyectoRepository repo) {
        this.repo = repo;
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
        return repo.save(p);
    }

    /** Cierra el proyecto: queda en solo lectura. Si no tenía fecha de fin real, se usa la de hoy. */
    @Transactional
    public Proyecto cerrar(Long id) {
        Proyecto p = obtenerEditable(id);
        p.estado = EstadoProyecto.CERRADO;
        p.cerradoEn = Instant.now();
        if (p.fechaFinReal == null) {
            p.fechaFinReal = LocalDate.now();
        }
        return repo.save(p);
    }

    /** Devuelve el proyecto o falla si no existe o ya está cerrado. Lo usan también la EDT y los demás módulos. */
    public Proyecto obtenerEditable(Long id) {
        Proyecto p = obtener(id);
        if (p.estado == EstadoProyecto.CERRADO) {
            throw ApiException.conflicto("PROYECTO_CERRADO", "El proyecto " + p.codigo + " está cerrado y no admite cambios.");
        }
        return p;
    }

    private void rechazarCierreDirecto(ProyectoRequest r) {
        if (r.estado() == EstadoProyecto.CERRADO) {
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
