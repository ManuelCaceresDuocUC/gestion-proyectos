package cl.proyecto.gestion.planificacion.calendario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NoLaborableRequest(
    Long proyectoId,
    @NotNull LocalDate fecha,
    @Size(max = 120) String descripcion) {}
