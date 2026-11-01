package cl.proyecto.gestion.cambio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Bitácora de un cambio en la solicitud (tabla "cambio_historial", V1).
 * C-04 solo escribe al cambiar el estado de una etapa. El GET es C-05.
 */
@Entity
@Table(name = "cambio_historial")
public class CambioHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "solicitud_id", nullable = false)
    public Long solicitudId;

    @Column(nullable = false, length = 40)
    public String campo;

    @Column(name = "valor_anterior")
    public String valorAnterior;

    @Column(name = "valor_nuevo")
    public String valorNuevo;

    @Column(name = "usuario_id")
    public Long usuarioId;

    public String comentario;

    @Column(nullable = false)
    public Instant fecha;

    @PrePersist
    void antesDeGuardar() {
        if (fecha == null) {
            fecha = Instant.now();
        }
    }
}
