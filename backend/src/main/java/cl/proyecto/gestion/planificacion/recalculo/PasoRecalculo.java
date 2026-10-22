package cl.proyecto.gestion.planificacion.recalculo;

/**
 * Un paso del recálculo del proyecto. Cada módulo registra su paso como @Component con @Order
 * (ver OrdenRecalculo). RecalculoService los ejecuta todos, en orden, dentro de una transacción.
 * Así cualquier cambio (duración, recursos, solicitud aprobada) se propaga en un solo lugar.
 */
public interface PasoRecalculo {
    void ejecutar(Long proyectoId);
}
