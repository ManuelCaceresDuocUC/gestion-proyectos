package cl.proyecto.gestion.proyecto;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API de la EDT (A-05). Contrato en docs/api/proyectos-edt.md. */
@RestController
@RequestMapping("/api")
public class TareaController {

    private final TareaService servicio;

    public TareaController(TareaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/proyectos/{proyectoId}/edt")
    public List<NodoEdt> arbol(@PathVariable Long proyectoId) {
        return servicio.arbol(proyectoId);
    }

    @PostMapping("/proyectos/{proyectoId}/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public TareaResponse crear(@PathVariable Long proyectoId, @Valid @RequestBody TareaRequest request) {
        return TareaResponse.de(servicio.crear(proyectoId, request));
    }

    @GetMapping("/tareas/{id}")
    public TareaResponse obtener(@PathVariable Long id) {
        return TareaResponse.de(servicio.obtener(id));
    }

    @PutMapping("/tareas/{id}")
    public TareaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TareaRequest request) {
        return TareaResponse.de(servicio.actualizar(id, request));
    }

    @DeleteMapping("/tareas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    @PostMapping("/tareas/{id}/mover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mover(@PathVariable Long id, @Valid @RequestBody MoverRequest request) {
        servicio.mover(id, request.posicion());
    }
}
