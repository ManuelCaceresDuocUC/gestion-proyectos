package cl.proyecto.gestion.hito;

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

/** API de hitos (B-11). Contrato en docs/api/hitos.md. */
@RestController
@RequestMapping("/api")
public class HitoController {

    private final HitoService servicio;

    public HitoController(HitoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/proyectos/{proyectoId}/hitos")
    public List<HitoResponse> listar(@PathVariable Long proyectoId) {
        return servicio.listar(proyectoId);
    }

    @PostMapping("/proyectos/{proyectoId}/hitos")
    @ResponseStatus(HttpStatus.CREATED)
    public HitoResponse crear(@PathVariable Long proyectoId, @Valid @RequestBody HitoRequest request) {
        return servicio.crear(proyectoId, request);
    }

    @GetMapping("/hitos/{id}")
    public HitoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/hitos/{id}")
    public HitoResponse actualizar(@PathVariable Long id, @Valid @RequestBody HitoRequest request) {
        return servicio.actualizar(id, request);
    }

    @DeleteMapping("/hitos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
