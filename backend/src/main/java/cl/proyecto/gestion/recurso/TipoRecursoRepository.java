package cl.proyecto.gestion.recurso;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoRecursoRepository extends JpaRepository<TipoRecurso, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<TipoRecurso> findByNombreIgnoreCase(String nombre);
}