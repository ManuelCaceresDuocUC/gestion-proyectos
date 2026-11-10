package cl.proyecto.gestion.tablero;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** C-12. JWT: autenticado. No recalcula. */
@RestController
public class TableroCambioController {

    private final TableroCambioService servicio;

    public TableroCambioController(TableroCambioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/tablero/cambio/{proyectoId}")
    public TableroCambioResponse cambio(@PathVariable Long proyectoId) {
        return servicio.deProyecto(proyectoId);
    }
}
