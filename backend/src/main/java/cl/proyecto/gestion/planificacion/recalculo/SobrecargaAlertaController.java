package cl.proyecto.gestion.planificacion.recalculo;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/sobreasignaciones")
public class SobrecargaAlertaController {

    private final SobrecargaAlertaService servicio;

    public SobrecargaAlertaController(SobrecargaAlertaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<SobrecargaAlertaResponse> listar(@PathVariable Long proyectoId) {
        return servicio.listar(proyectoId);
    }
}
