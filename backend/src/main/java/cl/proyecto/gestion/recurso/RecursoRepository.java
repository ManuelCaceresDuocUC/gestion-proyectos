package cl.proyecto.gestion.recurso;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findByActivoTrueOrderByNombre();

    List<Recurso> findAllByOrderByNombre();

    boolean existsByNombreIgnoreCase(String nombre);
}