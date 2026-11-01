package cl.proyecto.gestion.cambio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioEtapaRepository extends JpaRepository<CambioEtapa, Long> {

    List<CambioEtapa> findBySolicitudIdOrderByNumeroAsc(Long solicitudId);

    List<CambioEtapa> findBySolicitudIdIn(Collection<Long> solicitudIds);

    Optional<CambioEtapa> findBySolicitudIdAndNumero(Long solicitudId, short numero);
}
