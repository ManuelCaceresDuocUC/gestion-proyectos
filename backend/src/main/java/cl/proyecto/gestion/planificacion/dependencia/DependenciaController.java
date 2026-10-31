package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.planificacion.recalculo.ResultadoRecalculo;
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

/** API de dependencias (A-09, A-13). Contrato en docs/api/planificacion.md. */
@RestController
@RequestMapping("/api")
public class DependenciaController {

    private final DependenciaService servicio;

    public DependenciaController(DependenciaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/proyectos/{proyectoId}/dependencias")
    public List<DependenciaResponse> listar(@PathVariable Long proyectoId) {
        return servicio.listar(proyectoId);
    }

    @PostMapping("/proyectos/{proyectoId}/dependencias")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoDependencia crear(@PathVariable Long proyectoId, @Valid @RequestBody DependenciaRequest request) {
        return servicio.crear(proyectoId, request);
    }

    @PutMapping("/dependencias/{id}")
    public ResultadoDependencia actualizar(@PathVariable Long id, @Valid @RequestBody DependenciaEdicion request) {
        return servicio.actualizar(id, request);
    }

    @DeleteMapping("/dependencias/{id}")
    public ResultadoRecalculo eliminar(@PathVariable Long id) {
        return servicio.eliminar(id);
    }
}
