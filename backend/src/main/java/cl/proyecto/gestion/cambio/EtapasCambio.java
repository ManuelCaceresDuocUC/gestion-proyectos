package cl.proyecto.gestion.cambio;

/**
 * Catálogo fijo de las 10 etapas (C-04). El número y el nombre se copian a cambio_etapa
 * al crear la solicitud; no se persiste este enum.
 */
public enum EtapasCambio {
    NECESIDAD(1, "Necesidad"),
    EVALUACION(2, "Evaluación"),
    RIESGOS(3, "Riesgos"),
    PLANIFICACION(4, "Planificación"),
    COMUNICACION_INICIAL(5, "Comunicación inicial"),
    IMPLEMENTACION(6, "Implementación"),
    SEGUIMIENTO(7, "Seguimiento"),
    RESULTADOS_CORTO_PLAZO(8, "Resultados corto plazo"),
    CORRECTIVAS(9, "Correctivas"),
    ADOPCION_CULTURAL(10, "Adopción cultural");

    public final int numero;
    public final String nombre;

    EtapasCambio(int numero, String nombre) {
        this.numero = numero;
        this.nombre = nombre;
    }
}
