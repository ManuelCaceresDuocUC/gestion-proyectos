package cl.proyecto.gestion.recurso;

import cl.proyecto.gestion.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursoService {

    private final RecursoRepository repo;
    private final TipoRecursoRepository tipos;

    public RecursoService(RecursoRepository repo, TipoRecursoRepository tipos) {
        this.repo = repo;
        this.tipos = tipos;
    }

    @Transactional(readOnly = true)
    public List<Recurso> listar(boolean soloActivos) {
        return soloActivos
            ? repo.findByActivoTrueOrderByNombre()
            : repo.findAllByOrderByNombre();
    }

    @Transactional(readOnly = true)
    public Recurso obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe el recurso " + id + "."));
    }

    @Transactional
    public Recurso crear(RecursoRequest request) {
        validarTipo(request.tipoRecursoId());

        Recurso recurso = new Recurso();
        aplicar(recurso, request);

        return repo.save(recurso);
    }

    @Transactional
    public Recurso actualizar(Long id, RecursoRequest request) {
        Recurso recurso = obtener(id);
        validarTipo(request.tipoRecursoId());
        aplicar(recurso, request);

        return repo.save(recurso);
    }

    @Transactional
    public void eliminar(Long id) {
        Recurso recurso = obtener(id);
        repo.delete(recurso);
    }

    private void validarTipo(Long tipoRecursoId) {
        if (!tipos.existsById(tipoRecursoId)) {
            throw ApiException.invalido(
                "TIPO_RECURSO_INEXISTENTE",
                "No existe el tipo de recurso " + tipoRecursoId + ".");
        }
    }

    private void aplicar(Recurso recurso, RecursoRequest request) {
        recurso.tipoRecursoId = request.tipoRecursoId();
        recurso.nombre = request.nombre().trim();
        recurso.tarifa = request.tarifa();
        recurso.capacidadHorasDia = request.capacidadHorasDia();

        if (request.activo() != null) {
            recurso.activo = request.activo();
        }
    }
}