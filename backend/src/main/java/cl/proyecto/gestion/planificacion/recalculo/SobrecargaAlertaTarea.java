package cl.proyecto.gestion.planificacion.recalculo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "sobreasignacion_alerta_tarea")
@IdClass(SobrecargaAlertaTareaId.class)
public class SobrecargaAlertaTarea {

    @Id
    @Column(name = "alerta_id", nullable = false)
    public Long alertaId;

    @Id
    @Column(name = "tarea_id", nullable = false)
    public Long tareaId;

    @Column(name = "tarea_codigo", nullable = false, length = 20)
    public String tareaCodigo;

    @Column(name = "tarea_nombre", nullable = false, length = 200)
    public String tareaNombre;
}
