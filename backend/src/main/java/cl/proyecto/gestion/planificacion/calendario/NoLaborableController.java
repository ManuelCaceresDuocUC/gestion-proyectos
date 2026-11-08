package cl.proyecto.gestion.planificacion.calendario;

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
@RequestMapping("/api/no-laborables")
public class NoLaborableController {

    private final NoLaborableService servicio;

    public NoLaborableController(NoLaborableService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<NoLaborableResponse> listar(@RequestParam(required = false) Long proyectoId) {
        return servicio.listar(proyectoId).stream().map(NoLaborableResponse::de).toList();
    }

    @GetMapping("/{id}")
    public NoLaborableResponse obtener(@PathVariable Long id) {
        return NoLaborableResponse.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoLaborableResponse crear(@Valid @RequestBody NoLaborableRequest request) {
        return NoLaborableResponse.de(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public NoLaborableResponse actualizar(@PathVariable Long id,
                                          @Valid @RequestBody NoLaborableRequest request) {
        return NoLaborableResponse.de(servicio.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
