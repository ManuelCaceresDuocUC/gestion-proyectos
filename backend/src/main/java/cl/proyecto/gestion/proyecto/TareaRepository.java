package cl.proyecto.gestion.proyecto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByProyectoId(Long proyectoId);

    /** Etapas de un proyecto, en orden. */
    List<Tarea> findByProyectoIdAndParentIdIsNullOrderByOrden(Long proyectoId);

    /** Hijos directos de una tarea, en orden. */
    List<Tarea> findByParentIdOrderByOrden(Long parentId);
}
