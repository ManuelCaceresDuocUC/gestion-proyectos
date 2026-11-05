package cl.proyecto.gestion.recurso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoRecursoRequest(
    @NotBlank @Size(max = 60) String nombre,
    @NotBlank @Size(max = 20) String unidad) {}