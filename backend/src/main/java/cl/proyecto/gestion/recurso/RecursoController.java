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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recursos")
public class RecursoController {

    private final RecursoService servicio;

    public RecursoController(RecursoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RecursoResponse> listar(
        @RequestParam(defaultValue = "true") boolean soloActivos) {
        return servicio.listar(soloActivos).stream()
            .map(RecursoResponse::de)
            .toList();
    }

    @GetMapping("/{id}")
    public RecursoResponse obtener(@PathVariable Long id) {
        return RecursoResponse.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoResponse crear(@Valid @RequestBody RecursoRequest request) {
        return RecursoResponse.de(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public RecursoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody RecursoRequest request) {
        return RecursoResponse.de(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}