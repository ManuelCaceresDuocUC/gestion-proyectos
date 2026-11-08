package cl.proyecto.gestion.planificacion.recalculo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SobrecargaAlertaResponse(
    Long id,
    Long recursoId,
    String profesional,
    LocalDate fecha,
    BigDecimal cargaHoras,
    BigDecimal capacidadHorasDia,
    BigDecimal excesoHoras,
    List<TareaSobrecargaResponse> tareas) {

    public static SobrecargaAlertaResponse de(
        SobrecargaAlerta alerta,
        List<SobrecargaAlertaTarea> tareas) {
        return new SobrecargaAlertaResponse(
            alerta.id,
            alerta.recursoId,
            alerta.recursoNombre,
            alerta.fecha,
            alerta.cargaHoras,
            alerta.capacidadHorasDia,
            alerta.excesoHoras,
            tareas.stream().map(TareaSobrecargaResponse::de).toList());
    }
}
