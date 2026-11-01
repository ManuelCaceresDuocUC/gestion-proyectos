package cl.proyecto.gestion.cambio;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-03: crear, consultar, editar y borrar solicitudes. No aplica el cambio al plan (eso es C-09)
 * ni valida transiciones de estado (C-08).
 */
@Service
public class SolicitudCambioService {

    private final SolicitudCambioRepository repo;
    private final ProyectoService proyectos;
    private final TareaRepository tareas;

    public SolicitudCambioService(SolicitudCambioRepository repo, ProyectoService proyectos,
                                  TareaRepository tareas) {
        this.repo = repo;
        this.proyectos = proyectos;
        this.tareas = tareas;
    }

    @Transactional(readOnly = true)
    public SolicitudCambio obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la solicitud de cambio " + id + "."));
    }

    /**
     * Solicitudes del proyecto. Cada filtro nulo se ignora.
     * desde/hasta aplican a fechaSolicitud (inclusive).
     */
    @Transactional(readOnly = true)
    public List<SolicitudCambio> listar(Long proyectoId, EstadoSolicitud estado, TipoSolicitud tipo,
                                        PrioridadSolicitud prioridad, Long responsableId,
                                        LocalDate desde, LocalDate hasta) {
        proyectos.obtener(proyectoId);
        return repo.findByProyectoId(proyectoId).stream()
            .filter(s -> estado == null || s.estado == estado)
            .filter(s -> tipo == null || s.tipo == tipo)
            .filter(s -> prioridad == null || s.prioridad == prioridad)
            .filter(s -> responsableId == null || responsableId.equals(s.responsableId))
            .filter(s -> desde == null || (s.fechaSolicitud != null && !s.fechaSolicitud.isBefore(desde)))
            .filter(s -> hasta == null || (s.fechaSolicitud != null && !s.fechaSolicitud.isAfter(hasta)))
            .sorted(Comparator.comparing((SolicitudCambio s) -> s.fechaSolicitud,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    @Transactional
    public SolicitudCambio crear(Long proyectoId, SolicitudCambioRequest r) {
        proyectos.obtenerEditable(proyectoId);
        validarTarea(proyectoId, r.tareaId());
        SolicitudCambio s = new SolicitudCambio();
        s.proyectoId = proyectoId;
        s.codigo = siguienteCodigo(proyectoId);
        s.aplicada = false;
        s.estado = EstadoSolicitud.ABIERTO;
        aplicar(s, r, true);
        return repo.save(s);
    }

    @Transactional
    public SolicitudCambio actualizar(Long id, SolicitudCambioRequest r) {
        SolicitudCambio s = obtener(id);
        proyectos.obtenerEditable(s.proyectoId);
        validarTarea(s.proyectoId, r.tareaId());
        aplicar(s, r, false);
        return repo.save(s);
    }

    @Transactional
    public void eliminar(Long id) {
        SolicitudCambio s = obtener(id);
        proyectos.obtenerEditable(s.proyectoId);
        repo.delete(s);
    }

    private void validarTarea(Long proyectoId, Long tareaId) {
        if (tareaId == null) {
            return;
        }
        Tarea tarea = tareas.findById(tareaId)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la tarea " + tareaId + "."));
        if (!proyectoId.equals(tarea.proyectoId)) {
            throw ApiException.invalido("TAREA_DE_OTRO_PROYECTO",
                "La tarea " + tarea.codigo + " no pertenece a este proyecto.");
        }
    }

    private String siguienteCodigo(Long proyectoId) {
        int max = 0;
        for (SolicitudCambio s : repo.findByProyectoId(proyectoId)) {
            if (s.codigo != null && s.codigo.startsWith("SC-")) {
                try {
                    max = Math.max(max, Integer.parseInt(s.codigo.substring(3)));
                } catch (NumberFormatException ignored) {
                    // códigos que no siguen SC-n se ignoran
                }
            }
        }
        return "SC-%02d".formatted(max + 1);
    }

    private void aplicar(SolicitudCambio s, SolicitudCambioRequest r, boolean alta) {
        s.nombre = r.nombre().trim();
        s.descripcion = r.descripcion();
        s.solicitante = r.solicitante();
        s.responsableId = r.responsableId();
        if (r.fechaSolicitud() != null) {
            s.fechaSolicitud = r.fechaSolicitud();
        }
        s.inicioPrevisto = r.inicioPrevisto();
        s.finPrevisto = r.finPrevisto();
        s.finReal = r.finReal();
        s.tipo = r.tipo();
        if (r.prioridad() != null) {
            s.prioridad = r.prioridad();
        } else if (alta) {
            s.prioridad = PrioridadSolicitud.MEDIA;
        }
        if (r.riesgo() != null) {
            s.riesgo = r.riesgo();
        } else if (alta) {
            s.riesgo = RiesgoSolicitud.MEDIO;
        }
        if (r.impacto() != null) {
            s.impacto = r.impacto();
        } else if (alta) {
            s.impacto = ImpactoSolicitud.MEDIO;
        }
        if (!alta && r.estado() != null) {
            s.estado = r.estado();
        }
        s.tareaId = r.tareaId();
        if (r.impactoDias() != null) {
            s.impactoDias = r.impactoDias();
        } else if (alta) {
            s.impactoDias = 0;
        }
        if (r.impactoCosto() != null) {
            s.impactoCosto = r.impactoCosto();
        } else if (alta) {
            s.impactoCosto = BigDecimal.ZERO;
        }
        s.impactoAlcance = r.impactoAlcance();
        s.impactoRecursos = r.impactoRecursos();
        s.resultadoEvaluacion = r.resultadoEvaluacion();
        s.presupuestoPrevisto = r.presupuestoPrevisto();
        s.costoReal = r.costoReal();
        s.accionCorrectiva = r.accionCorrectiva();
        s.resultado = r.resultado();
    }
}
