package cl.proyecto.gestion.planificacion.calendario;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import java.util.HashSet;
import org.springframework.stereotype.Component;

/** Arma el calendario laboral de un proyecto: sus días de la semana más los no laborables generales y propios. */
@Component
public class CalendarioProyectos {

    private final NoLaborableRepository noLaborables;

    public CalendarioProyectos(NoLaborableRepository noLaborables) {
        this.noLaborables = noLaborables;
    }

    public Calendario de(Proyecto proyecto) {
        return Calendario.deProyecto(proyecto.diasLaborales, new HashSet<>(noLaborables.fechasDe(proyecto.id)));
    }
}
