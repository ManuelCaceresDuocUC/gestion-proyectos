package cl.proyecto.gestion.kpi;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiDefinicionRepository extends JpaRepository<KpiDefinicion, Long> {

    Optional<KpiDefinicion> findByProyectoIdIsNullAndCodigo(String codigo);

    List<KpiDefinicion> findByProyectoIdIsNullAndActivoTrueOrderByCodigoAsc();

    List<KpiDefinicion> findByProyectoIdAndActivoTrueOrderByCodigoAsc(Long proyectoId);
}
