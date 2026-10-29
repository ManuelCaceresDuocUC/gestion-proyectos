package cl.proyecto.gestion.proyecto;

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
 * Proyecto. Mapea la tabla "proyecto" (ver V1 y V20). Los campos son públicos a propósito (entidad simple,
 * sin Lombok); la API nunca expone la entidad: usa ProyectoResponse.
 * avance, costoPlan, costoReal y fechaFinPlan los calcula el recálculo (A-06, A-12), no se editan a mano.
 * responsableId es un id de recurso (módulo recurso, Dev B); se guarda como número para no acoplar módulos.
 */
@Entity
@Table(name = "proyecto")
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 30)
    public String codigo;

    @Column(nullable = false, length = 160)
    public String nombre;

    public String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoProyecto estado = EstadoProyecto.PLANIFICADO;

    @Column(name = "responsable_id")
    public Long responsableId;

    @Column(nullable = false, precision = 14, scale = 2)
    public BigDecimal presupuesto = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    public String moneda = "CLP";

    @Column(name = "dias_laborales", nullable = false, length = 7)
    public String diasLaborales = "LMXJV";

    @Column(name = "fecha_inicio")
    public LocalDate fechaInicio;

    @Column(name = "fecha_fin_plan")
    public LocalDate fechaFinPlan;

    @Column(name = "fecha_fin_base")
    public LocalDate fechaFinBase;

    @Column(name = "fecha_fin_real")
    public LocalDate fechaFinReal;

    @Column(nullable = false, precision = 5, scale = 2)
    public BigDecimal avance = BigDecimal.ZERO;

    @Column(name = "costo_plan", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoPlan = BigDecimal.ZERO;

    @Column(name = "costo_real", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoReal = BigDecimal.ZERO;

    @Column(name = "creado_en", nullable = false)
    public Instant creadoEn;

    @Column(name = "cerrado_en")
    public Instant cerradoEn;

    @PrePersist
    void antesDeGuardar() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
