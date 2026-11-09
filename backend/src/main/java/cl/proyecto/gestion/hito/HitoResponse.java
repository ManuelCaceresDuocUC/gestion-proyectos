package cl.proyecto.gestion.hito;

import java.time.LocalDate;
import java.util.List;

public record HitoResponse(
    Long id,
    Long proyectoId,
    String codigo,
    String logro,
    int semanaPrevista,
    LocalDate fechaPlan,
    LocalDate fechaBase,
    LocalDate fechaReal,
    EstadoHito estado,
    String evidencia,
    String indicador,
    String criterioAceptacion,
    String validador,
    String accionSiNoCumple,
    Long responsableId,
    List<Long> tareaIds,
    List<ActividadHitoResponse> actividades
) {}
