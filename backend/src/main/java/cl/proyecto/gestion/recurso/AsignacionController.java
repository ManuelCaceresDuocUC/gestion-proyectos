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
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    private final AsignacionService servicio;

    public AsignacionController(AsignacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/tarea/{tareaId}")
    public List<AsignacionResponse> listarPorTarea(@PathVariable Long tareaId) {
        return servicio.listarPorTarea(tareaId).stream()
            .map(AsignacionResponse::de)
            .toList();
    }

    @GetMapping("/{id}")
    public AsignacionResponse obtener(@PathVariable Long id) {
        return AsignacionResponse.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AsignacionResponse crear(@Valid @RequestBody AsignacionRequest request) {
        return AsignacionResponse.de(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public AsignacionResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody AsignacionRequest request) {
        return AsignacionResponse.de(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}