package cl.proyecto.gestion.hito;

import java.io.Serializable;
import java.util.Objects;

/** Clave de hito_tarea. Los nombres coinciden con los de la entidad. */
public class HitoTareaId implements Serializable {

    public Long hitoId;
    public Long tareaId;

    public HitoTareaId() {}

    public HitoTareaId(Long hitoId, Long tareaId) {
        this.hitoId = hitoId;
        this.tareaId = tareaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HitoTareaId otro)) {
            return false;
        }
        return Objects.equals(hitoId, otro.hitoId) && Objects.equals(tareaId, otro.tareaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hitoId, tareaId);
    }
}
