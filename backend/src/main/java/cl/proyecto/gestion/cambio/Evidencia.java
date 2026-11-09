package cl.proyecto.gestion.cambio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** Adjunto de una solicitud (tabla "evidencia", V1). Solo descripción y URL; no hay archivos. */
@Entity
@Table(name = "evidencia")
public class Evidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "solicitud_id", nullable = false)
    public Long solicitudId;

    @Column(nullable = false, length = 250)
    public String descripcion;

    @Column(length = 500)
    public String url;

    @Column(name = "creado_por")
    public Long creadoPor;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @PrePersist
    void antesDeGuardar() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
