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
import java.time.Instant;

/** Snapshot de un KPI en un recálculo (tabla {@code kpi_historico}, V1). Valor nulo = No calculable. */
@Entity
@Table(name = "kpi_historico")
public class KpiHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "kpi_id", nullable = false)
    public Long kpiId;

    @Column(name = "proyecto_id", nullable = false)
    public Long proyectoId;

    @Column(precision = 18, scale = 4)
    public BigDecimal valor;

    @Column(precision = 8, scale = 4)
    public BigDecimal cumplimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    public SemaforoKpi semaforo;

    @Column(length = 60)
    public String motivo;

    @Column(name = "calculado_en", nullable = false)
    public Instant calculadoEn;
}
