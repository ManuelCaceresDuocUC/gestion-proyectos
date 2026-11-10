package cl.proyecto.gestion.kpi;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiDefinicionRepository extends JpaRepository<KpiDefinicion, Long> {

    Optional<KpiDefinicion> findByProyectoIdIsNullAndCodigo(String codigo);

    Optional<KpiDefinicion> findByProyectoIdAndCodigo(Long proyectoId, String codigo);

    List<KpiDefinicion> findByProyectoIdIsNullOrderByCodigoAsc();

    List<KpiDefinicion> findByProyectoIdOrderByCodigoAsc(Long proyectoId);

    List<KpiDefinicion> findByProyectoIdIsNullAndActivoTrueOrderByCodigoAsc();

    List<KpiDefinicion> findByProyectoIdAndActivoTrueOrderByCodigoAsc(Long proyectoId);
}
