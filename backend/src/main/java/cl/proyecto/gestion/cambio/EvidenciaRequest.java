package cl.proyecto.gestion.cambio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de evidencia: descripción obligatoria; URL opcional (sin archivo). */
public record EvidenciaRequest(
    @NotBlank @Size(max = 250) String descripcion,
    @Size(max = 500) String url) {}
