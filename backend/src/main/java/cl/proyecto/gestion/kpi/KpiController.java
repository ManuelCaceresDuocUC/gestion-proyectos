package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.common.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @GetMapping("/api/kpi")
    public List<KpiDefinicionResponse> definiciones(@RequestParam(required = false) Long proyectoId) {
        if (proyectoId == null) {
            throw ApiException.invalido("PROYECTO_OBLIGATORIO", "Indique el proyecto del listado de KPI.");
        }
        return servicio.listarDefiniciones(proyectoId);
    }

    @PostMapping("/api/kpi")
    @ResponseStatus(HttpStatus.CREATED)
    public KpiDefinicionResponse crear(@Valid @RequestBody KpiDefinicionRequest request) {
        return servicio.crear(request);
    }

    @GetMapping("/api/kpi/{id}")
    public KpiDefinicionResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/api/kpi/{id}")
    public KpiDefinicionResponse actualizar(@PathVariable Long id, @Valid @RequestBody KpiDefinicionRequest request) {
        return servicio.actualizar(id, request);
    }

    @DeleteMapping("/api/kpi/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    @GetMapping("/api/kpi/{id}/historico")
    public List<KpiHistoricoResponse> historico(
            @PathVariable Long id, @RequestParam(required = false) Long proyectoId) {
        return servicio.historico(id, proyectoId);
    }
}
