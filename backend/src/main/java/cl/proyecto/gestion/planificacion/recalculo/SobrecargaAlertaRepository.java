package cl.proyecto.gestion.planificacion.recalculo;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SobrecargaAlertaRepository extends JpaRepository<SobrecargaAlerta, Long> {
    List<SobrecargaAlerta> findByProyectoIdOrderByFechaAscRecursoNombreAsc(Long proyectoId);
    void deleteByProyectoId(Long proyectoId);
}
