package cl.proyecto.gestion.kpi;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KpiController {

    private final KpiService servicio;

    public KpiController(KpiService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/proyectos/{proyectoId}/kpi")
    public List<KpiEstadoResponse> listar(@PathVariable Long proyectoId) {
        return servicio.listar(proyectoId);
    }

    @GetMapping("/api/kpi/{id}/historico")
    public List<KpiHistoricoResponse> historico(
            @PathVariable Long id, @RequestParam(required = false) Long proyectoId) {
        return servicio.historico(id, proyectoId);
    }
}
