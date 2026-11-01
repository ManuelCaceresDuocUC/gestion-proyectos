package cl.proyecto.gestion.cambio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudCambioRepository extends JpaRepository<SolicitudCambio, Long> {

    List<SolicitudCambio> findByProyectoId(Long proyectoId);

    boolean existsByProyectoIdAndCodigo(Long proyectoId, String codigo);
}
