package cl.proyecto.gestion.cambio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Una de las 10 etapas de una solicitud (tabla "cambio_etapa", V1).
 * solicitudId y actualizadoPor son números, sin relaciones JPA.
 */
@Entity
@Table(name = "cambio_etapa")
public class CambioEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "solicitud_id", nullable = false)
    public Long solicitudId;

    @Column(nullable = false)
    public short numero;

    @Column(nullable = false, length = 60)
    public String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    public EstadoEtapa estado = EstadoEtapa.PENDIENTE;

    @Column(name = "actualizado_en", nullable = false)
    public Instant actualizadoEn;

    @Column(name = "actualizado_por")
    public Long actualizadoPor;

    @PrePersist
    @PreUpdate
    void tocar() {
        actualizadoEn = Instant.now();
    }
}
