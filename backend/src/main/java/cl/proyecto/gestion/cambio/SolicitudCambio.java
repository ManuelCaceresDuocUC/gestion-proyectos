package cl.proyecto.gestion.cambio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Solicitud de cambio (tabla "solicitud_cambio", V1 + V21).
 * proyectoId, responsableId y tareaId se guardan como números, sin relaciones JPA.
 */
@Entity
@Table(name = "solicitud_cambio")
public class SolicitudCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id", nullable = false)
    public Long proyectoId;

    @Column(nullable = false, length = 20)
    public String codigo;

    @Column(nullable = false, length = 200)
    public String nombre;

    public String descripcion;

    @Column(length = 120)
    public String solicitante;

    @Column(name = "responsable_id")
    public Long responsableId;

    @Column(name = "fecha_solicitud", nullable = false)
    public LocalDate fechaSolicitud;

    @Column(name = "inicio_previsto")
    public LocalDate inicioPrevisto;

    @Column(name = "fin_previsto")
    public LocalDate finPrevisto;

    @Column(name = "fin_real")
    public LocalDate finReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public PrioridadSolicitud prioridad = PrioridadSolicitud.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public RiesgoSolicitud riesgo = RiesgoSolicitud.MEDIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public ImpactoSolicitud impacto = ImpactoSolicitud.MEDIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoSolicitud estado = EstadoSolicitud.ABIERTO;

    @Column(name = "aprobada_en")
    public Instant aprobadaEn;

    @Column(name = "tarea_id")
    public Long tareaId;

    @Column(name = "impacto_dias", nullable = false)
    public int impactoDias = 0;

    @Column(name = "impacto_costo", nullable = false, precision = 14, scale = 2)
    public BigDecimal impactoCosto = BigDecimal.ZERO;

    @Column(name = "impacto_alcance")
    public String impactoAlcance;

    @Column(name = "impacto_recursos")
    public String impactoRecursos;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_evaluacion", length = 15)
    public ResultadoEvaluacion resultadoEvaluacion;

    @Column(nullable = false)
    public boolean aplicada = false;

    @Column(name = "aplicada_en")
    public Instant aplicadaEn;

    @Column(name = "presupuesto_previsto", precision = 14, scale = 2)
    public BigDecimal presupuestoPrevisto;

    @Column(name = "costo_real", precision = 14, scale = 2)
    public BigDecimal costoReal;

    @Column(name = "accion_correctiva")
    public String accionCorrectiva;

    public String resultado;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @PrePersist
    void antesDeGuardar() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDate.now();
        }
    }
}
