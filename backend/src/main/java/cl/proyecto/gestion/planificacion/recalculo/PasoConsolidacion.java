package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Paso CONSOLIDACION (A-06): consolida hacia arriba avance y fechas (ver ConsolidadorEdt).
 * Corre una vez después de las fechas y otra después de los costos de las hojas (Dev B), por eso hay dos pasos.
 */
@Component
@Order(OrdenRecalculo.CONSOLIDACION)
public class PasoConsolidacion implements PasoRecalculo {

    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;
    private final ConsolidadorEdt consolidador = new ConsolidadorEdt();

    public PasoConsolidacion(ProyectoRepository proyectos, TareaRepository tareas) {
        this.proyectos = proyectos;
        this.tareas = tareas;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }
        var lista = tareas.findByProyectoId(proyectoId);
        consolidador.aplicar(proyecto, lista, new Calendario(Set.of()));
        tareas.saveAll(lista);
        proyectos.save(proyecto);
    }
}
