package cl.proyecto.gestion.proyecto;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de proyectos (A-04). Contrato en docs/api/proyectos-edt.md. */
@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService servicio;

    public ProyectoController(ProyectoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ProyectoResponse> listar(@RequestParam(required = false) EstadoProyecto estado,
                                         @RequestParam(required = false) Long responsableId) {
        return servicio.listar(estado, responsableId).stream().map(ProyectoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ProyectoResponse obtener(@PathVariable Long id) {
        return ProyectoResponse.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoResponse crear(@Valid @RequestBody ProyectoRequest request) {
        return ProyectoResponse.de(servicio.crear(request));
    }

    @PutMapping("/{id}")
    public ProyectoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoRequest request) {
        return ProyectoResponse.de(servicio.actualizar(id, request));
    }

    @PostMapping("/{id}/cerrar")
    public ProyectoResponse cerrar(@PathVariable Long id) {
        return ProyectoResponse.de(servicio.cerrar(id));
    }
}
