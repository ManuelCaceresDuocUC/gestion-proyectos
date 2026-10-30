package cl.proyecto.gestion.planificacion.calendario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Día no laborable (feriado, cierre). proyectoId nulo = aplica a todos los proyectos.
 * El CRUD de esta tabla es del issue B-07; el recálculo solo la lee.
 */
@Entity
@Table(name = "no_laborable")
public class NoLaborable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id")
    public Long proyectoId;

    @Column(nullable = false)
    public LocalDate fecha;

    @Column(length = 120)
    public String descripcion;
}
