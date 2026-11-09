package cl.proyecto.gestion.kpi;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiVariableRepository extends JpaRepository<KpiVariable, Long> {

    List<KpiVariable> findByKpiIdOrderByNombreAsc(Long kpiId);
}
