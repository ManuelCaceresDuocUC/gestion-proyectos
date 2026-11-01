package cl.proyecto.gestion.cambio;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

/** API de solicitudes de cambio (C-03). Contrato en docs/api/cambios.md. */
@RestController
@RequestMapping("/api")
public class SolicitudCambioController {

    private final SolicitudCambioService servicio;
    private final CambioEtapaService etapas;

    public SolicitudCambioController(SolicitudCambioService servicio, CambioEtapaService etapas) {
        this.servicio = servicio;
        this.etapas = etapas;
    }

    @GetMapping("/proyectos/{proyectoId}/cambios")
    public List<SolicitudCambioResponse> listar(@PathVariable Long proyectoId,
                                                @RequestParam(required = false) EstadoSolicitud estado,
                                                @RequestParam(required = false) TipoSolicitud tipo,
                                                @RequestParam(required = false) PrioridadSolicitud prioridad,
                                                @RequestParam(required = false) Long responsableId,
                                                @RequestParam(required = false) LocalDate desde,
                                                @RequestParam(required = false) LocalDate hasta) {
        List<SolicitudCambio> lista = servicio.listar(proyectoId, estado, tipo, prioridad, responsableId, desde, hasta);
        Map<Long, List<CambioEtapa>> porSolicitud = etapas.porSolicitudes(lista.stream().map(s -> s.id).toList());
        return lista.stream()
            .map(s -> SolicitudCambioResponse.de(s, porSolicitud.getOrDefault(s.id, List.of())))
            .toList();
    }

    @PostMapping("/proyectos/{proyectoId}/cambios")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudCambioResponse crear(@PathVariable Long proyectoId,
                                         @Valid @RequestBody SolicitudCambioRequest request) {
        return conEtapas(servicio.crear(proyectoId, request));
    }

    @GetMapping("/cambios/{id}")
    public SolicitudCambioResponse obtener(@PathVariable Long id) {
        return conEtapas(servicio.obtener(id));
    }

    @PutMapping("/cambios/{id}")
    public SolicitudCambioResponse actualizar(@PathVariable Long id,
                                              @Valid @RequestBody SolicitudCambioRequest request) {
        return conEtapas(servicio.actualizar(id, request));
    }

    @DeleteMapping("/cambios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    private SolicitudCambioResponse conEtapas(SolicitudCambio s) {
        return SolicitudCambioResponse.de(s, etapas.listar(s.id));
    }
}
