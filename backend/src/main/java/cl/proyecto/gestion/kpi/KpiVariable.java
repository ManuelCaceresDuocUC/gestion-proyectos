package cl.proyecto.gestion.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Nombre usado en la fórmula (tabla {@code kpi_variable}, V1). */
@Entity
@Table(name = "kpi_variable")
public class KpiVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "kpi_id", nullable = false)
    public Long kpiId;

    @Column(nullable = false, length = 60)
    public String nombre;

    @Column(length = 200)
    public String descripcion;
}
