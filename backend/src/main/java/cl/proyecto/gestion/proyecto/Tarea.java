package cl.proyecto.gestion.proyecto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nodo de la EDT: etapa, actividad o subactividad (tabla "tarea", jerarquía por parent_id).
 * Las ids de proyecto, padre y responsable se guardan como números (sin relaciones JPA) para no acoplar módulos.
 * Las fechas, el avance y los costos los calcula el recálculo (A-06, A-12, módulo recurso); solo
 * nombre, descripción, duración, responsable y avance de las hojas se editan a mano.
 */
@Entity
@Table(name = "tarea")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id", nullable = false)
    public Long proyectoId;

    @Column(name = "parent_id")
    public Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public TipoTarea tipo;

    /** Código EDT: 1, 1.1, 1.1.1. Único por proyecto. */
    @Column(nullable = false, length = 20)
    public String codigo;

    @Column(nullable = false, length = 200)
    public String nombre;

    public String descripcion;

    /** Posición entre sus hermanos (1, 2, 3...). El último número del código es siempre este valor. */
    @Column(nullable = false)
    public int orden;

    @Column(name = "responsable_id")
    public Long responsableId;

    /** Duración en días hábiles. En etapas y actividades con hijos se deriva de ellos (A-06). */
    @Column(name = "duracion_dias")
    public Integer duracionDias;

    @Column(name = "inicio_plan")
    public LocalDate inicioPlan;

    @Column(name = "fin_plan")
    public LocalDate finPlan;

    @Column(name = "inicio_base")
    public LocalDate inicioBase;

    @Column(name = "fin_base")
    public LocalDate finBase;

    @Column(name = "inicio_real")
    public LocalDate inicioReal;

    @Column(name = "fin_real")
    public LocalDate finReal;

    @Column(nullable = false, precision = 5, scale = 2)
    public BigDecimal avance = BigDecimal.ZERO;

    @Column(name = "costo_plan", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoPlan = BigDecimal.ZERO;

    @Column(name = "costo_real", nullable = false, precision = 14, scale = 2)
    public BigDecimal costoReal = BigDecimal.ZERO;

    @Column(name = "costo_base", precision = 14, scale = 2)
    public BigDecimal costoBase;
}
