package cl.proyecto.gestion.cambio;

import jakarta.validation.constraints.NotNull;

/** Body del PATCH de una etapa. */
public record CambioEtapaRequest(@NotNull EstadoEtapa estado) {}
