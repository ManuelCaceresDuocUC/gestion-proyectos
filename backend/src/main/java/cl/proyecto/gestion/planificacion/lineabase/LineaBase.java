package cl.proyecto.gestion.planificacion.lineabase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Una versión de la línea base de un proyecto (tabla "linea_base"). Los valores de la base por tarea viven en las
 * columnas *_base de "tarea" y se reemplazan al fijar una nueva versión; aquí queda el historial de versiones.
 */
@Entity
@Table(name = "linea_base")
public class LineaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id", nullable = false)
    public Long proyectoId;

    @Column(nullable = false)
    public int version;

    @Column(length = 200)
    public String motivo;

    @Column(name = "creada_en", nullable = false)
    public Instant creadaEn;

    /** Fecha de fin del proyecto al fijar esta versión. */
    @Column(name = "fecha_fin")
    public LocalDate fechaFin;

    /** Costo planificado total del proyecto al fijar esta versión. */
    @Column(name = "costo_total", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoTotal = BigDecimal.ZERO;

    @PrePersist
    void alCrear() {
        if (creadaEn == null) {
            creadaEn = Instant.now();
        }
    }
}
