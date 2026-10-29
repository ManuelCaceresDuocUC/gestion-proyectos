package cl.proyecto.gestion.proyecto;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    boolean existsByCodigo(String codigo);
}
