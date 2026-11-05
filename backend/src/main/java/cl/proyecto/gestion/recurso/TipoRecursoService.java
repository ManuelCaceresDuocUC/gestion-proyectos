package cl.proyecto.gestion.recurso;

import cl.proyecto.gestion.common.ApiException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoRecursoService {

    private final TipoRecursoRepository repo;

    public TipoRecursoService(TipoRecursoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<TipoRecurso> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public TipoRecurso obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe el tipo de recurso " + id + "."));
    }

    @Transactional
    public TipoRecurso crear(TipoRecursoRequest request) {
        String nombre = request.nombre().trim();

        if (repo.existsByNombreIgnoreCase(nombre)) {
            throw ApiException.conflicto(
                "TIPO_RECURSO_DUPLICADO",
                "Ya existe un tipo de recurso con el nombre " + nombre + ".");
        }

        TipoRecurso tipo = new TipoRecurso();
        tipo.nombre = nombre;
        tipo.unidad = request.unidad().trim();

        return repo.save(tipo);
    }

    @Transactional
    public TipoRecurso actualizar(Long id, TipoRecursoRequest request) {
        TipoRecurso tipo = obtener(id);
        String nombre = request.nombre().trim();

        repo.findByNombreIgnoreCase(nombre)
            .filter(otro -> !otro.id.equals(id))
            .ifPresent(otro -> {
                throw ApiException.conflicto(
                    "TIPO_RECURSO_DUPLICADO",
                    "Ya existe un tipo de recurso con el nombre " + nombre + ".");
            });

        tipo.nombre = nombre;
        tipo.unidad = request.unidad().trim();

        return repo.save(tipo);
    }

    @Transactional
    public void eliminar(Long id) {
        TipoRecurso tipo = obtener(id);

        try {
            repo.delete(tipo);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflicto(
                "TIPO_RECURSO_EN_USO",
                "No se puede eliminar el tipo de recurso porque tiene recursos asociados.");
        }
    }
}