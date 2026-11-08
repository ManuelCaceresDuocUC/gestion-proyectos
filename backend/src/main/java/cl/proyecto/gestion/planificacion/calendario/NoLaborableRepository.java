package cl.proyecto.gestion.planificacion.calendario;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoLaborableRepository extends JpaRepository<NoLaborable, Long> {

    boolean existsByProyectoIdAndFecha(Long proyectoId, LocalDate fecha);

    boolean existsByProyectoIdIsNullAndFecha(LocalDate fecha);

    boolean existsByProyectoIdAndFechaAndIdNot(Long proyectoId, LocalDate fecha, Long id);

    boolean existsByProyectoIdIsNullAndFechaAndIdNot(LocalDate fecha, Long id);

    List<NoLaborable> findByProyectoIdOrProyectoIdIsNullOrderByFecha(Long proyectoId);

    List<NoLaborable> findByProyectoIdIsNullOrderByFecha();

    /** Fechas no laborables que afectan a un proyecto: las generales y las propias. */
    @Query("select n.fecha from NoLaborable n where n.proyectoId is null or n.proyectoId = :proyectoId")
    List<LocalDate> fechasDe(@Param("proyectoId") Long proyectoId);
}
