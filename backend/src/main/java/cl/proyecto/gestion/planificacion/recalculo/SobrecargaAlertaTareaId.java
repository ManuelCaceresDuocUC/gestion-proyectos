package cl.proyecto.gestion.planificacion.recalculo;

import java.io.Serializable;

public class SobrecargaAlertaTareaId implements Serializable {
    public Long alertaId;
    public Long tareaId;

    public SobrecargaAlertaTareaId() {}

    public SobrecargaAlertaTareaId(Long alertaId, Long tareaId) {
        this.alertaId = alertaId;
        this.tareaId = tareaId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SobrecargaAlertaTareaId that)) return false;
        return java.util.Objects.equals(alertaId, that.alertaId)
            && java.util.Objects.equals(tareaId, that.tareaId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(alertaId, tareaId);
    }
}
