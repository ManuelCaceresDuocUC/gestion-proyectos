package cl.proyecto.gestion.kpi;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiHistoricoRepository extends JpaRepository<KpiHistorico, Long> {

    List<KpiHistorico> findByKpiIdAndProyectoIdOrderByCalculadoEnDescIdDesc(Long kpiId, Long proyectoId);

    List<KpiHistorico> findTop2ByKpiIdAndProyectoIdOrderByCalculadoEnDescIdDesc(Long kpiId, Long proyectoId);

    List<KpiHistorico> findByProyectoId(Long proyectoId);

    long countByKpiId(Long kpiId);
}
