package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.proyecto.ProyectoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SobrecargaAlertaService {

    private final SobrecargaAlertaRepository alertas;
    private final SobrecargaAlertaTareaRepository tareas;
    private final ProyectoService proyectos;

    public SobrecargaAlertaService(
        SobrecargaAlertaRepository alertas,
        SobrecargaAlertaTareaRepository tareas,
        ProyectoService proyectos) {
        this.alertas = alertas;
        this.tareas = tareas;
        this.proyectos = proyectos;
    }

    @Transactional(readOnly = true)
    public List<SobrecargaAlertaResponse> listar(Long proyectoId) {
        proyectos.obtener(proyectoId);
        return alertas.findByProyectoIdOrderByFechaAscRecursoNombreAsc(proyectoId).stream()
            .map(alerta -> SobrecargaAlertaResponse.de(
                alerta, tareas.findByAlertaIdOrderByTareaCodigo(alerta.id)))
            .toList();
    }
}
