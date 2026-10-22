package cl.proyecto.gestion.planificacion.motor;

/** Desfase en días hábiles; negativo = adelanto. */
public record DependenciaPlan(long predecesoraId, long sucesoraId, TipoDependencia tipo, int desfaseDias) {}
