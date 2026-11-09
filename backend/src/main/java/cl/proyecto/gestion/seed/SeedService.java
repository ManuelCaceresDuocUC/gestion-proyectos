package cl.proyecto.gestion.seed;

import cl.proyecto.gestion.cambio.SolicitudCambioRequest;
import cl.proyecto.gestion.cambio.SolicitudCambioService;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaRequest;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaService;
import cl.proyecto.gestion.planificacion.lineabase.LineaBaseService;
import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
import cl.proyecto.gestion.proyecto.EstadoProyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecurso;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-07: recorte v1 (proyecto, recursos, EDT, asignaciones).
 * C-11: dependencias, línea base y solicitudes. No duplica. Hitos y KPI quedan fuera.
 */
@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);
    static final String CODIGO = "EEC-2026";

    private final ProyectoRepository proyectosRepo;
    private final ProyectoService proyectos;
    private final TareaService tareas;
    private final TareaRepository tareasRepo;
    private final TipoRecursoService tipos;
    private final RecursoService recursos;
    private final AsignacionService asignaciones;
    private final DependenciaService dependencias;
    private final LineaBaseService lineasBase;
    private final SolicitudCambioService cambios;

    public SeedService(ProyectoRepository proyectosRepo, ProyectoService proyectos, TareaService tareas,
                       TareaRepository tareasRepo, TipoRecursoService tipos, RecursoService recursos,
                       AsignacionService asignaciones, DependenciaService dependencias,
                       LineaBaseService lineasBase, SolicitudCambioService cambios) {
        this.proyectosRepo = proyectosRepo;
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.tareasRepo = tareasRepo;
        this.tipos = tipos;
        this.recursos = recursos;
        this.asignaciones = asignaciones;
        this.dependencias = dependencias;
        this.lineasBase = lineasBase;
        this.cambios = cambios;
    }

    /** {@code true} si cargó v1 y/o v2; {@code false} si el caso ya estaba completo. */
    @Transactional
    public boolean cargar() {
        CasoPrueba c = CasoPruebaLector.leer();
        Long proyectoId;
        boolean v1 = false;
        if (proyectosRepo.existsByCodigo(c.proyecto().codigo())) {
            proyectoId = proyectosRepo.findByCodigo(c.proyecto().codigo()).orElseThrow().id;
            log.info("Seed C-07: el proyecto {} ya existe, no se repite.", c.proyecto().codigo());
        } else {
            Map<String, Long> tipoPorNombre = cargarTipos(c);
            Map<String, Long> recursoPorClave = cargarRecursos(c, tipoPorNombre);
            proyectoId = cargarProyecto(c.proyecto(), recursoPorClave);
            cargarEdt(proyectoId, c, recursoPorClave);
            log.info("Seed C-07: proyecto {} creado.", c.proyecto().codigo());
            v1 = true;
        }
        boolean deps = cargarDependencias(proyectoId, c);
        boolean base = fijarLineaBase(proyectoId, c);
        boolean sols = cargarSolicitudes(proyectoId, c);
        return v1 || deps || base || sols;
    }

    private boolean cargarDependencias(Long proyectoId, CasoPrueba c) {
        if (!dependencias.listar(proyectoId).isEmpty()) {
            log.info("Seed C-11: el proyecto {} ya tiene dependencias, no se repiten.", c.proyecto().codigo());
            return false;
        }
        Map<String, Long> idPorCodigo = mapaTareas(proyectoId);
        for (CasoDependencia d : c.dependencias()) {
            Long pred = idPorCodigo.get(d.predecesora());
            Long suc = idPorCodigo.get(d.sucesora());
            if (pred == null || suc == null) {
                throw new IllegalStateException(
                    "El seed referencia una tarea inexistente: " + d.predecesora() + " → " + d.sucesora());
            }
            dependencias.crear(proyectoId, new DependenciaRequest(
                pred, suc, TipoDependencia.valueOf(d.tipo()), d.desfaseDias()));
        }
        log.info("Seed C-11: {} dependencias creadas en {}.", c.dependencias().size(), c.proyecto().codigo());
        return true;
    }

    private boolean fijarLineaBase(Long proyectoId, CasoPrueba c) {
        if (!lineasBase.listar(proyectoId).isEmpty()) {
            log.info("Seed C-11: el proyecto {} ya tiene línea base, no se repite.", c.proyecto().codigo());
            return false;
        }
        lineasBase.fijar(proyectoId, "Seed C-11 caso de prueba");
        log.info("Seed C-11: línea base fijada en {}.", c.proyecto().codigo());
        return true;
    }

    private boolean cargarSolicitudes(Long proyectoId, CasoPrueba c) {
        if (!cambios.listar(proyectoId, null, null, null, null, null, null).isEmpty()) {
            log.info("Seed C-11: el proyecto {} ya tiene solicitudes, no se repiten.", c.proyecto().codigo());
            return false;
        }
        Map<String, Long> idPorCodigo = mapaTareas(proyectoId);
        Map<String, Long> recursoPorClave = mapaRecursosPorClave(c);
        for (CasoSolicitud s : c.solicitudesCambio()) {
            Long tareaId = s.tareaAfectada() == null ? null : idPorCodigo.get(s.tareaAfectada());
            if (s.tareaAfectada() != null && tareaId == null) {
                throw new IllegalStateException(
                    "El seed referencia una tarea inexistente: " + s.tareaAfectada());
            }
            Integer dias = s.impactoEstimado() == null ? null : s.impactoEstimado().dias();
            BigDecimal costo = s.impactoEstimado() == null ? null : s.impactoEstimado().costo();
            cambios.crear(proyectoId, new SolicitudCambioRequest(
                s.nombre(), s.justificacion(), null,
                s.responsable() == null ? null : recursoPorClave.get(s.responsable()),
                c.proyecto().fechaInicio(), null, null, null,
                SeedMapeo.prioridadDe(s.prioridad()), SeedMapeo.tipoDe(s.tipo()),
                SeedMapeo.riesgoDe(s.riesgo()), SeedMapeo.impactoDe(s.impacto()),
                null, tareaId, dias, costo, null, null, null, costo, null, null, null));
        }
        log.info("Seed C-11: {} solicitudes creadas en {}.", c.solicitudesCambio().size(), c.proyecto().codigo());
        return true;
    }

    private Map<String, Long> mapaTareas(Long proyectoId) {
        Map<String, Long> ids = new HashMap<>();
        for (Tarea t : tareasRepo.findByProyectoId(proyectoId)) {
            ids.put(t.codigo, t.id);
        }
        return ids;
    }

    private Map<String, Long> mapaRecursosPorClave(CasoPrueba c) {
        Map<String, Long> ids = new HashMap<>();
        Map<String, Long> porNombre = new HashMap<>();
        for (Recurso r : recursos.listar(false)) {
            porNombre.putIfAbsent(r.nombre, r.id);
        }
        for (CasoProfesional p : c.profesionales()) {
            Long id = porNombre.get(p.nombre());
            if (id != null) {
                ids.put(p.clave(), id);
            }
        }
        for (CasoRecurso rec : c.recursos()) {
            Long id = porNombre.get(rec.nombre());
            if (id != null) {
                ids.put(rec.clave(), id);
            }
        }
        return ids;
    }

    private Map<String, Long> cargarTipos(CasoPrueba c) {
        Map<String, Long> ids = new HashMap<>();
        for (TipoRecurso ya : tipos.listar()) {
            ids.put(ya.nombre, ya.id);
        }
        for (CasoTipoRecurso t : c.tiposRecurso()) {
            Long id = ids.get(t.nombre());
            if (id == null) {
                id = tipos.crear(new TipoRecursoRequest(t.nombre(), t.unidad())).id;
                ids.put(t.nombre(), id);
            }
        }
        return ids;
    }

    private Map<String, Long> cargarRecursos(CasoPrueba c, Map<String, Long> tipoPorNombre) {
        Map<String, Long> ids = new HashMap<>();
        Long tipoProfesional = tipoPorNombre.get("Profesional");
        for (CasoProfesional p : c.profesionales()) {
            Recurso r = recursos.crear(new RecursoRequest(
                tipoProfesional, p.nombre(), p.tarifaHora(), p.capacidadHorasDia(), true));
            ids.put(p.clave(), r.id);
        }
        for (CasoRecurso rec : c.recursos()) {
            Long tipoId = tipoPorNombre.get(rec.tipo());
            Recurso r = recursos.crear(new RecursoRequest(
                tipoId, rec.nombre(), rec.costoUnitario(), null, true));
            ids.put(rec.clave(), r.id);
        }
        return ids;
    }

    private Long cargarProyecto(CasoProyecto p, Map<String, Long> recursoPorClave) {
        Long responsableId = p.responsable() == null ? null : recursoPorClave.get(p.responsable());
        EstadoProyecto estado = p.estado() == null ? null : EstadoProyecto.valueOf(p.estado());
        return proyectos.crear(new ProyectoRequest(
            p.codigo(), p.nombre(), p.descripcion(), estado, responsableId,
            p.presupuesto(), p.moneda(), p.diasLaborales(), p.fechaInicio())).id;
    }

    private void cargarEdt(Long proyectoId, CasoPrueba c, Map<String, Long> recursoPorClave) {
        for (CasoEtapa etapa : c.estructura()) {
            Long etapaId = tareas.crear(proyectoId,
                new TareaRequest(null, etapa.nombre(), etapa.objetivo(), null, null, null)).id;
            for (CasoActividad act : etapa.actividades()) {
                if (act.esHoja()) {
                    Long tareaId = tareas.crear(proyectoId, new TareaRequest(
                        etapaId, act.nombre(), null,
                        CasoPruebaLector.diasDe(act.duracionSemanas()), null, avance(act.avance()))).id;
                    cargarAsignaciones(tareaId, act.asignaciones(), recursoPorClave);
                    continue;
                }
                Long actId = tareas.crear(proyectoId,
                    new TareaRequest(etapaId, act.nombre(), null, null, null, null)).id;
                for (CasoSubactividad sub : act.subactividades()) {
                    Long tareaId = tareas.crear(proyectoId, new TareaRequest(
                        actId, sub.nombre(), null,
                        CasoPruebaLector.diasDe(sub.duracionSemanas()), null, avance(sub.avance()))).id;
                    cargarAsignaciones(tareaId, sub.asignaciones(), recursoPorClave);
                }
            }
        }
    }

    private void cargarAsignaciones(
        Long tareaId,
        java.util.List<CasoAsignacion> definiciones,
        Map<String, Long> recursoPorClave) {
        for (CasoAsignacion definicion : definiciones) {
            Long recursoId = recursoPorClave.get(definicion.recurso());
            if (recursoId == null) {
                throw new IllegalStateException(
                    "El seed referencia un recurso inexistente: " + definicion.recurso());
            }
            asignaciones.crear(new AsignacionRequest(
                tareaId, recursoId, definicion.cantidad(), null, null, null));
        }
    }

    private static BigDecimal avance(Integer avance) {
        return avance == null ? null : BigDecimal.valueOf(avance);
    }
}
