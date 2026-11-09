package cl.proyecto.gestion.cambio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    List<Evidencia> findBySolicitudIdOrderByCreadoEnAsc(Long solicitudId);
}
