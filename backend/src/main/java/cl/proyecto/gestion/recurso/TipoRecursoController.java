package cl.proyecto.gestion.recurso;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tipos-recurso")
public class TipoRecursoController {

    private final TipoRecursoService servicio;

    public TipoRecursoController(TipoRecursoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<TipoRecursoResponse> listar() {
        return servicio.listar().stream()
            .map(TipoRecursoResponse::de)
            .toList();
    }

    @GetMapping("/{id}")
    public TipoRecursoResponse obtener(@PathVariable Long id) {
        return TipoRecursoResponse.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoRecursoResponse crear(@Valid @RequestBody TipoRecursoRequest request) {
        return TipoRecursoResponse.de(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public TipoRecursoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody TipoRecursoRequest request) {
        return TipoRecursoResponse.de(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}