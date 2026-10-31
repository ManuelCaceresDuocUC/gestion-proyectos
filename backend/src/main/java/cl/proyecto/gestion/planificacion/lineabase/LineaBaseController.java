package cl.proyecto.gestion.planificacion.lineabase;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de línea base y comparación plan vs real (A-14). Contrato en docs/api/linea-base.md. */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}")
public class LineaBaseController {

    private final LineaBaseService servicio;

    public LineaBaseController(LineaBaseService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/linea-base")
    @ResponseStatus(HttpStatus.CREATED)
    public LineaBaseResponse fijar(@PathVariable Long proyectoId,
                                   @Valid @RequestBody(required = false) LineaBaseRequest request) {
        return LineaBaseResponse.de(servicio.fijar(proyectoId, request == null ? null : request.motivo()));
    }

    @GetMapping("/lineas-base")
    public List<LineaBaseResponse> listar(@PathVariable Long proyectoId) {
        return servicio.listar(proyectoId).stream().map(LineaBaseResponse::de).toList();
    }

    @GetMapping("/comparacion")
    public ComparacionPlan comparar(@PathVariable Long proyectoId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hoy) {
        return servicio.comparar(proyectoId, hoy);
    }
}
