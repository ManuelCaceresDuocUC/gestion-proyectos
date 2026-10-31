package cl.proyecto.gestion.planificacion.lineabase;

import jakarta.validation.constraints.Size;

/** Motivo opcional de la nueva versión (por ejemplo "Aprobación de la solicitud SC-01"). */
public record LineaBaseRequest(@Size(max = 200) String motivo) {}
