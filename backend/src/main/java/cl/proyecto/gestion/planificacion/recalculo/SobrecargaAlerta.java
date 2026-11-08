package cl.proyecto.gestion.planificacion.recalculo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sobreasignacion_alerta")
public class SobrecargaAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id", nullable = false)
    public Long proyectoId;

    @Column(name = "recurso_id", nullable = false)
    public Long recursoId;

    @Column(name = "recurso_nombre", nullable = false, length = 160)
    public String recursoNombre;

    @Column(nullable = false)
    public LocalDate fecha;

    @Column(name = "carga_horas", nullable = false, precision = 12, scale = 4)
    public BigDecimal cargaHoras;

    @Column(name = "capacidad_horas_dia", nullable = false, precision = 4, scale = 1)
    public BigDecimal capacidadHorasDia;

    @Column(name = "exceso_horas", nullable = false, precision = 12, scale = 4)
    public BigDecimal excesoHoras;
}
