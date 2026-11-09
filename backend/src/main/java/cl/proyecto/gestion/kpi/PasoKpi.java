package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.planificacion.recalculo.OrdenRecalculo;
import cl.proyecto.gestion.planificacion.recalculo.PasoRecalculo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Paso 60: evalúa los KPI activos y agrega un snapshot por cada uno. */
@Component
@Order(OrdenRecalculo.KPI)
public class PasoKpi implements PasoRecalculo {

    private final KpiService kpi;

    public PasoKpi(KpiService kpi) {
        this.kpi = kpi;
    }

    @Override
    public void ejecutar(Long proyectoId) {
        kpi.registrarSnapshot(proyectoId);
    }
}
