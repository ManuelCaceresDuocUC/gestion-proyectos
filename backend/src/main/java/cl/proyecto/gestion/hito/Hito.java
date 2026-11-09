package cl.proyecto.gestion.hito;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Logro verificable de duración cero (tabla "hito"). No es una tarea de la EDT.
 * Las actividades asociadas viven en hito_tarea. fecha_plan es el último día hábil de la semana prevista.
 */
@Entity
@Table(name = "hito")
public class Hito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id", nullable = false)
    public Long proyectoId;

    @Column(nullable = false, length = 10)
    public String codigo;

    @Column(nullable = false, length = 250)
    public String logro;

    @Column(name = "fecha_plan")
    public LocalDate fechaPlan;

    @Column(name = "fecha_base")
    public LocalDate fechaBase;

    @Column(name = "fecha_real")
    public LocalDate fechaReal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public EstadoHito estado = EstadoHito.PENDIENTE;

    /** Evidencia y fuente de datos, en un solo texto (hoja «Definir hitos»). */
    public String evidencia;

    /** Indicador o forma de cálculo. */
    public String indicador;

    /** Unidad y criterio de aceptación. */
    @Column(name = "criterio_aceptacion")
    public String criterioAceptacion;

    /** Quién valida y cuándo. */
    @Column(length = 160)
    public String validador;

    @Column(name = "accion_si_no_cumple")
    public String accionSiNoCumple;

    @Column(name = "responsable_id")
    public Long responsableId;
}
