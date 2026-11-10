package cl.proyecto.gestion.tablero;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lectura del tablero de proyecto (B-21 / C-13). Contrato en docs/api/tableros.md. */
@RestController
@RequestMapping("/api/tablero/proyecto")
public class TableroProyectoController {

    private final TableroProyectoService servicio;

    public TableroProyectoController(TableroProyectoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{proyectoId}")
    public TableroProyectoResponse obtener(
            @PathVariable Long proyectoId,
            @RequestParam(required = false) Long etapaId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) Long responsableId) {
        return servicio.obtener(proyectoId, etapaId, desde, hasta, responsableId);
    }
}
