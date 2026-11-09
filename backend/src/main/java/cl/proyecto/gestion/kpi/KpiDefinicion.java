package cl.proyecto.gestion.kpi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Definición de KPI (tabla {@code kpi_definicion}, V1).
 * {@code proyectoId} nulo es una definición global. Las columnas de V21 no se mapean:
 * la base les pone el default al insertar.
 */
@Entity
@Table(name = "kpi_definicion")
public class KpiDefinicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "proyecto_id")
    public Long proyectoId;

    @Column(nullable = false, length = 20)
    public String codigo;

    @Column(nullable = false, length = 200)
    public String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public CategoriaKpi categoria;

    public String descripcion;

    @Column(nullable = false)
    public String formula;

    @Column(length = 20)
    public String unidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public SentidoKpi sentido;

    @Enumerated(EnumType.STRING)
    @Column(name = "operador_meta", nullable = false, length = 15)
    public OperadorMeta operadorMeta;

    @Column(precision = 18, scale = 4)
    public BigDecimal meta;

    @Column(name = "meta_max", precision = 18, scale = 4)
    public BigDecimal metaMax;

    @Column(name = "umbral_verde", nullable = false, precision = 6, scale = 4)
    public BigDecimal umbralVerde = BigDecimal.ONE;

    @Column(name = "umbral_amarillo", nullable = false, precision = 6, scale = 4)
    public BigDecimal umbralAmarillo = new BigDecimal("0.8000");

    @Column(length = 120)
    public String responsable;

    @Column(nullable = false)
    public boolean activo = true;
}
