package cl.proyecto.gestion.cambio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioHistorialRepository extends JpaRepository<CambioHistorial, Long> {

    List<CambioHistorial> findBySolicitudIdOrderByFechaAsc(Long solicitudId);
}
