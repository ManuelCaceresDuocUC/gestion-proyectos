package cl.proyecto.gestion.recurso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "asignacion")
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "tarea_id", nullable = false)
    public Long tareaId;

    @Column(name = "recurso_id", nullable = false)
    public Long recursoId;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal cantidad;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal tarifa;

    @Column(name = "horas_semana", precision = 5, scale = 1)
    public BigDecimal horasSemana;

    @Column(precision = 5, scale = 1)
    public BigDecimal semanas;

    @Column(name = "costo_plan", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoPlan = BigDecimal.ZERO;

    @Column(name = "cantidad_real", nullable = false, precision = 12, scale = 2)
    public BigDecimal cantidadReal = BigDecimal.ZERO;

    @Column(name = "costo_real", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoReal = BigDecimal.ZERO;
}