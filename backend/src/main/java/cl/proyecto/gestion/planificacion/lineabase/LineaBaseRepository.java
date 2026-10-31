package cl.proyecto.gestion.planificacion.lineabase;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineaBaseRepository extends JpaRepository<LineaBase, Long> {

    List<LineaBase> findByProyectoIdOrderByVersionDesc(Long proyectoId);

    Optional<LineaBase> findFirstByProyectoIdOrderByVersionDesc(Long proyectoId);
}
