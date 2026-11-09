package cl.proyecto.gestion.hito;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Actividad de la que depende un hito. La fecha del hito no puede ser anterior a su término. */
@Entity
@Table(name = "hito_tarea")
@IdClass(HitoTareaId.class)
public class HitoTarea {

    @Id
    @Column(name = "hito_id", nullable = false)
    public Long hitoId;

    @Id
    @Column(name = "tarea_id", nullable = false)
    public Long tareaId;
}
