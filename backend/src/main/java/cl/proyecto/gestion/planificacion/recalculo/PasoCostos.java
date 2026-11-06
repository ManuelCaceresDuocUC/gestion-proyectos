package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(OrdenRecalculo.COSTOS)
public class PasoCostos implements PasoRecalculo {

    private final TareaRepository tareas;
    private final AsignacionRepository asignaciones;

    public PasoCostos(
        TareaRepository tareas,
        AsignacionRepository asignaciones) {
        this.tareas = tareas;
        this.asignaciones = asignaciones;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        List<Tarea> tareasProyecto = tareas.findByProyectoId(proyectoId);
        List<Asignacion> asignacionesProyecto = new ArrayList<>();

        for (Tarea tarea : tareasProyecto) {
            if (tareas.existsByParentId(tarea.id)) {
                continue;
            }

            List<Asignacion> asignacionesTarea =
                asignaciones.findByTareaIdOrderById(tarea.id);
            asignacionesProyecto.addAll(asignacionesTarea);

            for (Asignacion asignacion : asignacionesTarea) {
                asignacion.costoPlan = costoPlan(asignacion)
                    .setScale(2, RoundingMode.HALF_UP);
                asignacion.costoReal = costoReal(asignacion)
                    .setScale(2, RoundingMode.HALF_UP);
            }

            tarea.costoPlan = asignacionesTarea.stream()
                .map(this::costoPlan)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

            tarea.costoReal = asignacionesTarea.stream()
                .map(this::costoReal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        }

        asignaciones.saveAll(asignacionesProyecto);
        tareas.saveAll(tareasProyecto);
    }

    private BigDecimal costoPlan(Asignacion asignacion) {
        return asignacion.cantidad
            .multiply(asignacion.tarifa);
    }

    private BigDecimal costoReal(Asignacion asignacion) {
        BigDecimal cantidadReal = asignacion.cantidadReal == null
            ? BigDecimal.ZERO
            : asignacion.cantidadReal;

        return cantidadReal.multiply(asignacion.tarifa);
    }
}