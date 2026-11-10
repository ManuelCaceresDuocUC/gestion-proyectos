package cl.proyecto.gestion.kpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Alta y edición de una definición (B-15). No dispara el recálculo ni escribe histórico. */
public record KpiDefinicionRequest(
        Long proyectoId,
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull CategoriaKpi categoria,
        @NotBlank String formula,
        @Size(max = 20) String unidad,
        @NotNull SentidoKpi sentido,
        @NotNull OperadorMeta operadorMeta,
        BigDecimal meta,
        BigDecimal metaMax,
        BigDecimal umbralVerde,
        BigDecimal umbralAmarillo,
        @Size(max = 120) String responsable,
        Boolean activo) {
}
