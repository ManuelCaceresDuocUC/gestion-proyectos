package cl.proyecto.gestion.seed;

import cl.proyecto.gestion.proyecto.EstadoProyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
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
 * C-07: carga el recorte v1 del caso de prueba usando los servicios (fechas las calcula el motor).
 * Incluye asignaciones para la demo de B-09. Si ya existe el proyecto {@code EEC-2026}, no hace nada.
 */
@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);
    static final String CODIGO = "EEC-2026";

    private final ProyectoRepository proyectosRepo;
    private final ProyectoService proyectos;
    private final TareaService tareas;
    private final TipoRecursoService tipos;
    private final RecursoService recursos;
    private final AsignacionService asignaciones;

    public SeedService(ProyectoRepository proyectosRepo, ProyectoService proyectos, TareaService tareas,
                       TipoRecursoService tipos, RecursoService recursos, AsignacionService asignaciones) {
        this.proyectosRepo = proyectosRepo;
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.tipos = tipos;
        this.recursos = recursos;
        this.asignaciones = asignaciones;
    }

    /** {@code true} si cargó el caso; {@code false} si {@code EEC-2026} ya estaba. */
    @Transactional
    public boolean cargar() {
        CasoPrueba c = CasoPruebaLector.leer();
        if (proyectosRepo.existsByCodigo(c.proyecto().codigo())) {
            log.info("Seed C-07: el proyecto {} ya existe, no se repite.", c.proyecto().codigo());
            return false;
        }
        Map<String, Long> tipoPorNombre = cargarTipos(c);
        Map<String, Long> recursoPorClave = cargarRecursos(c, tipoPorNombre);
        Long proyectoId = cargarProyecto(c.proyecto(), recursoPorClave);
        cargarEdt(proyectoId, c, recursoPorClave);
        log.info("Seed C-07: proyecto {} creado.", c.proyecto().codigo());
        return true;
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
