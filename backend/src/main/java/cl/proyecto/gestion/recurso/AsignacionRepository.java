package cl.proyecto.gestion.recurso;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByTareaIdOrderById(Long tareaId);

    List<Asignacion> findByRecursoIdOrderById(Long recursoId);

    Optional<Asignacion> findByTareaIdAndRecursoId(Long tareaId, Long recursoId);

    boolean existsByTareaIdAndRecursoId(Long tareaId, Long recursoId);
}