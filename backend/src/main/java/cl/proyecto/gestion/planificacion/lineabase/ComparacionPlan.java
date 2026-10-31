package cl.proyecto.gestion.planificacion.lineabase;

import java.time.Instant;
import java.util.List;

/** Comparación base / plan / real de todo el proyecto. versionLineaBase es nula si aún no se fija una base. */
public record ComparacionPlan(
    Integer versionLineaBase, Instant fijadaEn, ResumenComparacion resumen, List<FilaComparacion> tareas) {}
