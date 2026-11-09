package cl.proyecto.gestion.hito;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HitoTareaRepository extends JpaRepository<HitoTarea, HitoTareaId> {

    List<HitoTarea> findByHitoIdIn(Collection<Long> hitoIds);

    void deleteByHitoId(Long hitoId);
}
