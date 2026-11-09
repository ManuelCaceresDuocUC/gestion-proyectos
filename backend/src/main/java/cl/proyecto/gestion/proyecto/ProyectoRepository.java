package cl.proyecto.gestion.proyecto;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    boolean existsByCodigo(String codigo);

    Optional<Proyecto> findByCodigo(String codigo);
}
