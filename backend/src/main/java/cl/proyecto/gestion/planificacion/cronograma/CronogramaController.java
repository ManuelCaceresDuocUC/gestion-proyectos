package cl.proyecto.gestion.planificacion.cronograma;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PATCH de duración, fechas reales y avance (A-13). Contrato en docs/api/planificacion.md. */
@RestController
@RequestMapping("/api")
public class CronogramaController {

    private final CronogramaService servicio;

    public CronogramaController(CronogramaService servicio) {
        this.servicio = servicio;
    }

    @PatchMapping("/tareas/{id}")
    public ResultadoTarea parchear(@PathVariable Long id, @Valid @RequestBody TareaPatch patch) {
        return servicio.parchear(id, patch);
    }
}
