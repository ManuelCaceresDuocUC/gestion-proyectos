package cl.proyecto.gestion.planificacion.cronograma;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.recalculo.InstantaneaFechas;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TareaResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue A-13: cambia duración, fechas reales y avance de una tarea hoja y recalcula el proyecto.
 * Las etapas y actividades con hijos no se tocan a mano: sus valores se calculan desde los hijos.
 */
@Service
public class CronogramaService {

    private final TareaRepository tareas;
    private final ProyectoService proyectos;
    private final RecalculoService recalculo;

    public CronogramaService(TareaRepository tareas, ProyectoService proyectos, RecalculoService recalculo) {
        this.tareas = tareas;
        this.proyectos = proyectos;
        this.recalculo = recalculo;
    }

    @Transactional
    public ResultadoTarea parchear(Long id, TareaPatch p) {
        Tarea t = tareas.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la tarea " + id + "."));
        proyectos.obtenerEditable(t.proyectoId);
        if (p.duracionDias() == null && p.inicioReal() == null && p.finReal() == null && p.avance() == null) {
            throw ApiException.invalido("PATCH_VACIO",
                "Indique al menos uno de: duracionDias, inicioReal, finReal, avance.");
        }
        if (tareas.existsByParentId(t.id)) {
            throw ApiException.conflicto("TAREA_CON_HIJOS",
                t.codigo + " " + t.nombre + " tiene subtareas: su duración, fechas y avance se calculan desde ellas.");
        }
        LocalDate inicioReal = p.inicioReal() != null ? p.inicioReal() : t.inicioReal;
        LocalDate finReal = p.finReal() != null ? p.finReal() : t.finReal;
        if (inicioReal != null && finReal != null && finReal.isBefore(inicioReal)) {
            throw ApiException.invalido("FECHAS_REALES_INVALIDAS", "La fecha real de término es anterior a la de inicio.");
        }

        InstantaneaFechas antes = InstantaneaFechas.tomar(tareas.findByProyectoId(t.proyectoId));
        if (p.duracionDias() != null) t.duracionDias = p.duracionDias();
        if (p.avance() != null) t.avance = p.avance();
        t.inicioReal = inicioReal;
        t.finReal = finReal;
        tareas.save(t);
        recalculo.recalcular(t.proyectoId);
        return new ResultadoTarea(TareaResponse.de(t), antes.cambiosHasta(tareas.findByProyectoId(t.proyectoId)));
    }
}
