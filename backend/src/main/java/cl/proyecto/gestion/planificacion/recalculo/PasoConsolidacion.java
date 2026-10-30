package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.TareaRepository;
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
    private final CalendarioProyectos calendarios;
    private final ConsolidadorEdt consolidador = new ConsolidadorEdt();

    public PasoConsolidacion(ProyectoRepository proyectos, TareaRepository tareas, CalendarioProyectos calendarios) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.calendarios = calendarios;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        Proyecto proyecto = proyectos.findById(proyectoId).orElse(null);
        if (proyecto == null) {
            return;
        }
        var lista = tareas.findByProyectoId(proyectoId);
        consolidador.aplicar(proyecto, lista, calendarios.de(proyecto));
        tareas.saveAll(lista);
        proyectos.save(proyecto);
    }
}
