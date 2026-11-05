package cl.proyecto.gestion.recurso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "recurso")
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "tipo_recurso_id", nullable = false)
    public Long tipoRecursoId;

    @Column(nullable = false, length = 160)
    public String nombre;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal tarifa;

    @Column(name = "capacidad_horas_dia", precision = 4, scale = 1)
    public BigDecimal capacidadHorasDia;

    @Column(nullable = false)
    public boolean activo = true;
}
