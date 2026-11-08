package cl.proyecto.gestion.planificacion.recalculo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SobrecargaAlertaTareaRepository
    extends JpaRepository<SobrecargaAlertaTarea, SobrecargaAlertaTareaId> {

    List<SobrecargaAlertaTarea> findByAlertaIdOrderByTareaCodigo(Long alertaId);
}
