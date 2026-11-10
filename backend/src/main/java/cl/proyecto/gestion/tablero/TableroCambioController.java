package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** C-12 / C-13. JWT: autenticado. No recalcula. Query opcional: nulo = tablero entero. */
@RestController
public class TableroCambioController {

    private final TableroCambioService servicio;

    public TableroCambioController(TableroCambioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/tablero/cambio/{proyectoId}")
    public TableroCambioResponse cambio(
            @PathVariable Long proyectoId,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) PrioridadSolicitud prioridad,
            @RequestParam(required = false) RiesgoSolicitud riesgo,
            @RequestParam(required = false) Long responsableId) {
        return servicio.deProyecto(proyectoId, estado, prioridad, riesgo, responsableId);
    }
}
