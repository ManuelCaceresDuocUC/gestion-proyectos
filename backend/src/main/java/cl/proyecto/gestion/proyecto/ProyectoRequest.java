package cl.proyecto.gestion.proyecto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos editables de un proyecto (se usa en POST y PUT).
 * estado, moneda y diasLaborales son opcionales: si no vienen se usa PLANIFICADO, CLP y LMXJV (en PUT se mantiene el actual).
 * Para cerrar un proyecto se usa POST /api/proyectos/{id}/cerrar; aquí estado=TERMINADO se rechaza.
 */
public record ProyectoRequest(
    @NotBlank @Size(max = 30) String codigo,
    @NotBlank @Size(max = 160) String nombre,
    String descripcion,
    EstadoProyecto estado,
    Long responsableId,
    @NotNull @DecimalMin("0.0") BigDecimal presupuesto,
    @Size(min = 3, max = 3) String moneda,
    @Pattern(regexp = "[LMXJVSD]{1,7}") String diasLaborales,
    LocalDate fechaInicio) {}
