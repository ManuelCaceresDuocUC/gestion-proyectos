package cl.proyecto.gestion.cambio;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.cronograma.CronogramaService;
import cl.proyecto.gestion.planificacion.cronograma.TareaPatch;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-03: crear, consultar, editar y borrar solicitudes. Al crear, C-04 genera las 10 etapas.
 * El PUT valida el grafo de estado (C-08). Aprobar/rechazar son POST dedicados.
 * aplicarAlPlan (C-09) exige APROBADO, no aplicada y proyecto editable.
 * Tipo TIEMPO suma impactoDias a la duración de la hoja vía CronogramaService (él dispara el recálculo).
 * Al aplicar: aplicada, aplicadaEn, estado EN_IMPLEMENTACION e historial.
 */
@Service
public class SolicitudCambioService {

    private final SolicitudCambioRepository repo;
    private final ProyectoService proyectos;
    private final TareaRepository tareas;
    private final CambioEtapaService etapas;
    private final CambioHistorialRepository historial;
    private final CronogramaService cronograma;

    public SolicitudCambioService(SolicitudCambioRepository repo, ProyectoService proyectos,
                                  TareaRepository tareas, CambioEtapaService etapas,
                                  CambioHistorialRepository historial, CronogramaService cronograma) {
        this.repo = repo;
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.etapas = etapas;
        this.historial = historial;
        this.cronograma = cronograma;
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
        s = repo.save(s);
        etapas.generar(s.id);
        return s;
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

    /** Solo desde EN_EVALUACION. Marca {@code aprobadaEn}; no pone {@code aplicada} (C-09). */
    @Transactional
    public SolicitudCambio aprobar(Long id) {
        SolicitudCambio s = obtener(id);
        proyectos.obtenerEditable(s.proyectoId);
        if (!TransicionesSolicitud.puedeAprobar(s.estado)) {
            throw ApiException.conflicto("TRANSICION_INVALIDA",
                "Solo se puede aprobar una solicitud en evaluación.");
        }
        pasarA(s, EstadoSolicitud.APROBADO);
        return repo.save(s);
    }

    /** Solo desde EN_EVALUACION. */
    @Transactional
    public SolicitudCambio rechazar(Long id) {
        SolicitudCambio s = obtener(id);
        proyectos.obtenerEditable(s.proyectoId);
        if (!TransicionesSolicitud.puedeRechazar(s.estado)) {
            throw ApiException.conflicto("TRANSICION_INVALIDA",
                "Solo se puede rechazar una solicitud en evaluación.");
        }
        pasarA(s, EstadoSolicitud.RECHAZADO);
        return repo.save(s);
    }

    /**
     * Solo desde {@code APROBADO}, si aún no está aplicada y el proyecto admite cambios.
     * Tipo {@code TIEMPO}: nueva duración = duración actual + {@code impactoDias}, vía
     * {@link CronogramaService#parchear} (ese servicio ya recalcula; no se llama otra vez).
     * Los demás tipos no mueven el plan vigente.
     * Después marca {@code aplicada}/{@code aplicadaEn} y pasa a {@code EN_IMPLEMENTACION}.
     */
    @Transactional
    public SolicitudCambio aplicarAlPlan(Long id) {
        SolicitudCambio s = obtener(id);
        proyectos.obtenerEditable(s.proyectoId);
        exigirAplicable(s);
        aplicarImpactoAlPlan(s);
        marcarAplicada(s);
        return repo.save(s);
    }

    private void exigirAplicable(SolicitudCambio s) {
        if (s.aplicada) {
            throw ApiException.conflicto("YA_APLICADA",
                "La solicitud " + s.codigo + " ya fue aplicada al plan.");
        }
        if (!TransicionesSolicitud.puedeAplicar(s.estado)) {
            throw ApiException.conflicto("TRANSICION_INVALIDA",
                "Solo se puede aplicar una solicitud aprobada.");
        }
    }

    private void aplicarImpactoAlPlan(SolicitudCambio s) {
        if (s.tipo != TipoSolicitud.TIEMPO) {
            return;
        }
        if (s.tareaId == null) {
            throw ApiException.invalido("TAREA_REQUERIDA",
                "Una solicitud de tipo TIEMPO necesita la tarea a la que se aplica el plazo.");
        }
        Tarea tarea = resolverTarea(s.proyectoId, s.tareaId);
        int actual = tarea.duracionDias == null ? 0 : tarea.duracionDias;
        int nueva = actual + s.impactoDias;
        if (nueva < 0) {
            throw ApiException.invalido("DURACION_INVALIDA",
                "El impacto de " + s.impactoDias + " días dejaría la duración de "
                    + tarea.codigo + " en " + nueva + ".");
        }
        cronograma.parchear(s.tareaId, new TareaPatch(nueva, null, null, null));
    }

    private void marcarAplicada(SolicitudCambio s) {
        s.aplicada = true;
        if (s.aplicadaEn == null) {
            s.aplicadaEn = Instant.now();
        }
        pasarA(s, EstadoSolicitud.EN_IMPLEMENTACION);
    }

    private void validarTarea(Long proyectoId, Long tareaId) {
        if (tareaId == null) {
            return;
        }
        resolverTarea(proyectoId, tareaId);
    }

    private Tarea resolverTarea(Long proyectoId, Long tareaId) {
        Tarea tarea = tareas.findById(tareaId)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la tarea " + tareaId + "."));
        if (!proyectoId.equals(tarea.proyectoId)) {
            throw ApiException.invalido("TAREA_DE_OTRO_PROYECTO",
                "La tarea " + tarea.codigo + " no pertenece a este proyecto.");
        }
        return tarea;
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
            pasarA(s, r.estado());
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

    private void pasarA(SolicitudCambio s, EstadoSolicitud nuevo) {
        TransicionesSolicitud.exigir(s.estado, nuevo);
        if (s.estado == nuevo) {
            return;
        }
        EstadoSolicitud anterior = s.estado;
        s.estado = nuevo;
        if (nuevo == EstadoSolicitud.APROBADO && s.aprobadaEn == null) {
            s.aprobadaEn = Instant.now();
        }
        registrarEstado(s.id, anterior, nuevo);
    }

    private void registrarEstado(Long solicitudId, EstadoSolicitud anterior, EstadoSolicitud nuevo) {
        CambioHistorial h = new CambioHistorial();
        h.solicitudId = solicitudId;
        h.campo = "estado";
        h.valorAnterior = anterior.name();
        h.valorNuevo = nuevo.name();
        historial.save(h);
    }
}
