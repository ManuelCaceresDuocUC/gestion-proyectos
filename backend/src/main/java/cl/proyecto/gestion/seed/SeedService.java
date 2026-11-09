package cl.proyecto.gestion.seed;

import cl.proyecto.gestion.auth.RolUsuario;
import cl.proyecto.gestion.auth.Usuario;
import cl.proyecto.gestion.auth.UsuarioRepository;
import cl.proyecto.gestion.cambio.SolicitudCambioRequest;
import cl.proyecto.gestion.cambio.SolicitudCambioService;
import cl.proyecto.gestion.hito.EstadoHito;
import cl.proyecto.gestion.hito.HitoRequest;
import cl.proyecto.gestion.hito.HitoService;
import cl.proyecto.gestion.hito.SemanaHito;
import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaRequest;
import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaService;
import cl.proyecto.gestion.planificacion.lineabase.LineaBaseService;
import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
import cl.proyecto.gestion.proyecto.EstadoProyecto;
import cl.proyecto.gestion.proyecto.Proyecto;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-07: recorte v1 (proyecto, recursos, EDT, asignaciones).
 * C-11: dependencias, línea base y solicitudes.
 * B-11: hitos H1–H6 en PENDIENTE. C-01: usuarios demo, idempotente por email. No duplica. KPI queda fuera.
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
    private final HitoService hitos;
    private final CalendarioProyectos calendarios;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public SeedService(ProyectoRepository proyectosRepo, ProyectoService proyectos, TareaService tareas,
                       TareaRepository tareasRepo, TipoRecursoService tipos, RecursoService recursos,
                       AsignacionService asignaciones, DependenciaService dependencias,
                       LineaBaseService lineasBase, SolicitudCambioService cambios, HitoService hitos,
                       CalendarioProyectos calendarios, UsuarioRepository usuarios, PasswordEncoder encoder) {
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
        this.hitos = hitos;
        this.calendarios = calendarios;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    /** {@code true} si cargó usuarios, v1 y/o v2; {@code false} si el caso ya estaba completo. */
    @Transactional
    public boolean cargar() {
        CasoPrueba c = CasoPruebaLector.leer();
        boolean users = cargarUsuarios(c);
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
        boolean hitosCargados = cargarHitos(proyectoId, c);
        return v1 || deps || base || sols || hitosCargados || users;
    }

    /** C-01: por email. Si ya está, no reescribe el hash. No registra contraseña ni hash. */
    private boolean cargarUsuarios(CasoPrueba c) {
        int creados = 0;
        for (CasoUsuario dato : c.usuarios()) {
            if (dato.email() == null || dato.email().isBlank()) {
                throw new IllegalStateException("El seed trae un usuario sin email.");
            }
            String email = dato.email().trim().toLowerCase();
            if (usuarios.existsByEmail(email)) {
                log.info("Seed C-01: el usuario {} ya existe, no se repite.", email);
                continue;
            }
            if (dato.password() == null || dato.password().isBlank()) {
                throw new IllegalStateException("El seed no trae contraseña para " + email + ".");
            }
            Usuario u = new Usuario();
            u.nombre = dato.nombre() == null || dato.nombre().isBlank() ? email : dato.nombre().trim();
            u.email = email;
            u.passwordHash = encoder.encode(dato.password());
            u.rol = rolDe(dato.rol(), email);
            u.activo = true;
            usuarios.save(u);
            creados++;
            log.info("Seed C-01: usuario {} ({}) creado.", email, u.rol);
        }
        return creados > 0;
    }

    private static RolUsuario rolDe(String rol, String email) {
        if (rol == null || rol.isBlank()) {
            throw new IllegalStateException("El seed no trae rol para " + email + ".");
        }
        try {
            return RolUsuario.valueOf(rol.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Rol de seed no válido para " + email + ": " + rol.trim() + ".");
        }
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

    /** B-11: H1–H6 quedan PENDIENTE, con la ficha del JSON y sin fecha real. */
    private boolean cargarHitos(Long proyectoId, CasoPrueba c) {
        if (hitos.hayEn(proyectoId)) {
            log.info("Seed B-11: el proyecto {} ya tiene hitos, no se repiten.", c.proyecto().codigo());
            return false;
        }
        Map<String, Long> idPorCodigo = mapaTareas(proyectoId);
        Proyecto proyecto = proyectos.obtener(proyectoId);
        for (CasoHito h : c.hitos()) {
            List<Long> tareaIds = new ArrayList<>();
            LocalDate finMasTarde = null;
            for (String codigo : h.actividades()) {
                Long tareaId = idPorCodigo.get(codigo);
                if (tareaId == null) {
                    throw new IllegalStateException("El seed referencia una tarea inexistente: " + codigo);
                }
                tareaIds.add(tareaId);
                Tarea tarea = tareasRepo.findById(tareaId).orElseThrow();
                if (tarea.finPlan != null && (finMasTarde == null || tarea.finPlan.isAfter(finMasTarde))) {
                    finMasTarde = tarea.finPlan;
                }
            }
            int semana = semanaQueRespetaElTermino(proyecto, h.semanaPrevista(), finMasTarde);
            if (semana != h.semanaPrevista()) {
                log.info("Seed B-11: {} queda en la semana {} (el JSON decía {}) porque sus actividades terminan el {}.",
                    h.codigo(), semana, h.semanaPrevista(), finMasTarde);
            }
            hitos.crear(proyectoId, new HitoRequest(
                h.codigo(), h.logro(), semana, h.evidencia(), h.indicador(), h.criterio(),
                h.validador(), h.accionSiNoCumple(), EstadoHito.PENDIENTE, null, null, tareaIds));
        }
        log.info("Seed B-11: {} hitos creados en {}.", c.hitos().size(), c.proyecto().codigo());
        return true;
    }

    /**
     * La semana del JSON es la propuesta de la planilla. Si el motor dejó el término después
     * del viernes de esa semana, el hito se ubica en la semana de ese término.
     */
    private int semanaQueRespetaElTermino(Proyecto proyecto, Integer semanaJson, LocalDate finMasTarde) {
        int semana = semanaJson == null ? 1 : semanaJson;
        if (finMasTarde == null || proyecto.fechaInicio == null) {
            return semana;
        }
        Calendario calendario = calendarios.de(proyecto);
        LocalDate fecha = SemanaHito.fechaDeSemana(proyecto.fechaInicio, calendario, semana);
        if (!fecha.isBefore(finMasTarde)) {
            return semana;
        }
        return SemanaHito.semanaDe(proyecto.fechaInicio, calendario, finMasTarde);
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
