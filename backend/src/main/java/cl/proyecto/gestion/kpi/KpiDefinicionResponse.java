package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;
import java.util.List;

/** Definición guardada, con los nombres que usa la fórmula. El semáforo se lee en el listado del proyecto. */
public record KpiDefinicionResponse(
        Long id,
        Long proyectoId,
        String codigo,
        String nombre,
        String categoria,
        String descripcion,
        String formula,
        String unidad,
        String sentido,
        String operadorMeta,
        BigDecimal meta,
        BigDecimal metaMax,
        BigDecimal umbralVerde,
        BigDecimal umbralAmarillo,
        String responsable,
        boolean activo,
        List<String> variables) {
}
