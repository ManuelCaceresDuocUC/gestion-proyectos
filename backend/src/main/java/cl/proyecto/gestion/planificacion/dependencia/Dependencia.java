package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Precedencia entre dos tareas hoja (tabla "dependencia"). El CRUD y el rechazo de ciclos llegan con A-09. */
@Entity
@Table(name = "dependencia")
public class Dependencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "predecesora_id", nullable = false)
    public Long predecesoraId;

    @Column(name = "sucesora_id", nullable = false)
    public Long sucesoraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    public TipoDependencia tipo;

    /** Días hábiles; negativo = adelanto. */
    @Column(name = "desfase_dias", nullable = false)
    public int desfaseDias;
}
