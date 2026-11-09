package cl.proyecto.gestion.hito;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HitoRepository extends JpaRepository<Hito, Long> {

    List<Hito> findByProyectoIdOrderByFechaPlanAscCodigoAsc(Long proyectoId);

    boolean existsByProyectoId(Long proyectoId);

    boolean existsByProyectoIdAndCodigo(Long proyectoId, String codigo);
}
