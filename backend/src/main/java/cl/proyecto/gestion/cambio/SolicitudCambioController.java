package cl.proyecto.gestion.cambio;

import jakarta.validation.Valid;
import java.time.LocalDate;
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

/** API de solicitudes de cambio (C-03). Contrato en docs/api/cambios.md (tarea 9). */
@RestController
@RequestMapping("/api")
public class SolicitudCambioController {

    private final SolicitudCambioService servicio;

    public SolicitudCambioController(SolicitudCambioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/proyectos/{proyectoId}/cambios")
    public List<SolicitudCambioResponse> listar(@PathVariable Long proyectoId,
                                                @RequestParam(required = false) EstadoSolicitud estado,
                                                @RequestParam(required = false) TipoSolicitud tipo,
                                                @RequestParam(required = false) PrioridadSolicitud prioridad,
                                                @RequestParam(required = false) Long responsableId,
                                                @RequestParam(required = false) LocalDate desde,
                                                @RequestParam(required = false) LocalDate hasta) {
        return servicio.listar(proyectoId, estado, tipo, prioridad, responsableId, desde, hasta).stream()
            .map(SolicitudCambioResponse::de)
            .toList();
    }

    @PostMapping("/proyectos/{proyectoId}/cambios")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudCambioResponse crear(@PathVariable Long proyectoId,
                                         @Valid @RequestBody SolicitudCambioRequest request) {
        return SolicitudCambioResponse.de(servicio.crear(proyectoId, request));
    }

    @GetMapping("/cambios/{id}")
    public SolicitudCambioResponse obtener(@PathVariable Long id) {
        return SolicitudCambioResponse.de(servicio.obtener(id));
    }

    @PutMapping("/cambios/{id}")
    public SolicitudCambioResponse actualizar(@PathVariable Long id,
                                              @Valid @RequestBody SolicitudCambioRequest request) {
        return SolicitudCambioResponse.de(servicio.actualizar(id, request));
    }

    @DeleteMapping("/cambios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
