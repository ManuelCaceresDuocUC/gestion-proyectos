package cl.proyecto.gestion.planificacion.dependencia;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DependenciaRepository extends JpaRepository<Dependencia, Long> {

    /** Dependencias cuyas dos puntas pertenecen al proyecto. */
    @Query("select d from Dependencia d where d.sucesoraId in "
        + "(select t.id from Tarea t where t.proyectoId = :proyectoId)")
    List<Dependencia> findByProyecto(@Param("proyectoId") Long proyectoId);

    boolean existsByPredecesoraIdOrSucesoraId(Long predecesoraId, Long sucesoraId);
}
