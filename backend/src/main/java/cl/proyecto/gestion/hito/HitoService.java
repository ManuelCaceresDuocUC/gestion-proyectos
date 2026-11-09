package cl.proyecto.gestion.hito;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TipoTarea;
import cl.proyecto.gestion.recurso.RecursoRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B-11: ficha del hito y actividades asociadas.
 * Crear o editar no llama a RecalculoService y no mueve el Gantt. El paso HITOS queda para B-16.
 */
@Service
public class HitoService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final HitoRepository repo;
    private final HitoTareaRepository vinculos;
    private final ProyectoService proyectos;
    private final TareaRepository tareas;
    private final RecursoRepository recursos;
    private final CalendarioProyectos calendarios;

    public HitoService(HitoRepository repo, HitoTareaRepository vinculos, ProyectoService proyectos,
                       TareaRepository tareas, RecursoRepository recursos, CalendarioProyectos calendarios) {
        this.repo = repo;
        this.vinculos = vinculos;
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.recursos = recursos;
        this.calendarios = calendarios;
    }

    @Transactional(readOnly = true)
    public List<HitoResponse> listar(Long proyectoId) {
        Proyecto p = proyectos.obtener(proyectoId);
        return responder(p, repo.findByProyectoIdOrderByFechaPlanAscCodigoAsc(proyectoId));
    }

    @Transactional(readOnly = true)
    public HitoResponse obtener(Long id) {
        Hito h = obtenerEntidad(id);
        Proyecto p = proyectos.obtener(h.proyectoId);
        return responder(p, List.of(h)).getFirst();
    }

    @Transactional(readOnly = true)
    public boolean hayEn(Long proyectoId) {
        return repo.existsByProyectoId(proyectoId);
    }

    @Transactional
    public HitoResponse crear(Long proyectoId, HitoRequest r) {
        Proyecto p = proyectos.obtenerEditable(proyectoId);
        Preparado datos = preparar(p, r);
        String codigo = r.codigo().trim();
        if (repo.existsByProyectoIdAndCodigo(proyectoId, codigo)) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un hito con el código " + codigo + ".");
        }
        Hito h = new Hito();
        h.proyectoId = proyectoId;
        aplicar(h, r, codigo, datos.fechaPlan());
        repo.saveAndFlush(h);
        guardarTareas(h.id, datos.actividades());
        return obtener(h.id);
    }

    @Transactional
    public HitoResponse actualizar(Long id, HitoRequest r) {
        Hito h = obtenerEntidad(id);
        Proyecto p = proyectos.obtenerEditable(h.proyectoId);
        Preparado datos = preparar(p, r);
        String codigo = r.codigo().trim();
        if (!codigo.equals(h.codigo) && repo.existsByProyectoIdAndCodigo(h.proyectoId, codigo)) {
            throw ApiException.conflicto("CODIGO_DUPLICADO", "Ya existe un hito con el código " + codigo + ".");
        }
        aplicar(h, r, codigo, datos.fechaPlan());
        repo.save(h);
        guardarTareas(h.id, datos.actividades());
        return obtener(h.id);
    }

    @Transactional
    public void eliminar(Long id) {
        Hito h = obtenerEntidad(id);
        proyectos.obtenerEditable(h.proyectoId);
        vinculos.deleteByHitoId(h.id);
        vinculos.flush();
        repo.delete(h);
    }

    private Hito obtenerEntidad(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe el hito " + id + "."));
    }

    private Preparado preparar(Proyecto p, HitoRequest r) {
        if (p.fechaInicio == null) {
            throw ApiException.invalido("PROYECTO_SIN_INICIO",
                "El proyecto no tiene fecha de inicio, así que no se puede calcular la semana prevista.");
        }
        if (r.responsableId() != null && !recursos.existsById(r.responsableId())) {
            throw ApiException.noEncontrado("No existe el recurso " + r.responsableId() + ".");
        }
        List<Tarea> actividades = actividadesDe(p.id, r.tareaIds());
        Calendario calendario = calendarios.de(p);
        LocalDate fechaPlan = SemanaHito.fechaDeSemana(p.fechaInicio, calendario, r.semanaPrevista());
        String evidencia = blanco(r.evidencia());
        if (r.estado() == EstadoHito.CUMPLIDO) {
            if (evidencia == null) {
                throw ApiException.invalido("CUMPLIDO_SIN_EVIDENCIA",
                    "No se puede marcar el hito como cumplido sin evidencia.");
            }
            if (r.fechaReal() == null) {
                throw ApiException.invalido("CUMPLIDO_SIN_FECHA_REAL",
                    "No se puede marcar el hito como cumplido sin fecha real.");
            }
            exigirTerminadas(actividades);
        }
        exigirPosterior(fechaPlan, r.semanaPrevista(), actividades, true);
        if (r.fechaReal() != null) {
            exigirPosterior(r.fechaReal(), null, actividades, false);
        }
        return new Preparado(fechaPlan, actividades);
    }

    private List<Tarea> actividadesDe(Long proyectoId, List<Long> ids) {
        List<Long> unicos = ids.stream().distinct().toList();
        Map<Long, Tarea> porId = new HashMap<>();
        for (Tarea t : tareas.findAllById(unicos)) {
            porId.put(t.id, t);
        }
        List<Tarea> orden = new ArrayList<>();
        for (Long id : unicos) {
            Tarea t = porId.get(id);
            if (t == null) {
                throw ApiException.noEncontrado("No existe la actividad " + id + ".");
            }
            if (!proyectoId.equals(t.proyectoId)) {
                throw ApiException.invalido("TAREA_AJENA",
                    "La actividad " + t.codigo + " no pertenece a este proyecto.");
            }
            if (t.tipo == TipoTarea.ETAPA) {
                throw ApiException.invalido("HITO_ETAPA_NO_ADMITIDA",
                    "El hito solo puede asociarse a actividades, no a la etapa " + t.codigo + ".");
            }
            if (t.finPlan == null) {
                throw ApiException.invalido("HITO_SIN_TERMINO",
                    "La actividad " + t.codigo + " no tiene fecha de término planificada.");
            }
            orden.add(t);
        }
        return orden;
    }

    private void exigirTerminadas(List<Tarea> actividades) {
        List<String> pendientes = actividades.stream()
            .filter(t -> t.finReal == null)
            .map(t -> t.codigo + " " + t.nombre)
            .toList();
        if (!pendientes.isEmpty()) {
            String verbo = pendientes.size() == 1 ? "todavía no tiene" : "todavía no tienen";
            throw ApiException.conflicto("HITO_ACTIVIDADES_PENDIENTES",
                "No se puede marcar el hito como cumplido: "
                    + String.join("; ", pendientes) + " " + verbo + " fecha real de término.");
        }
    }

    /**
     * La fecha prevista se compara con el fin planificado.
     * La fecha real se compara con el fin real si ya existe, y si no con el fin planificado.
     */
    private void exigirPosterior(LocalDate fecha, Integer semana, List<Tarea> actividades, boolean prevista) {
        List<String> alertas = new ArrayList<>();
        for (Tarea t : actividades) {
            LocalDate termino = prevista || t.finReal == null ? t.finPlan : t.finReal;
            if (fecha.isBefore(termino)) {
                alertas.add(t.codigo + " " + t.nombre + " (termina el " + FECHA.format(termino) + ")");
            }
        }
        if (alertas.isEmpty()) {
            return;
        }
        String cual = prevista
            ? "La fecha prevista " + FECHA.format(fecha) + " (semana " + semana + ")"
            : "La fecha real " + FECHA.format(fecha);
        throw ApiException.conflicto("HITO_ANTES_DE_ACTIVIDADES",
            cual + " es anterior al término de " + String.join("; ", alertas) + ".");
    }

    private void aplicar(Hito h, HitoRequest r, String codigo, LocalDate fechaPlan) {
        h.codigo = codigo;
        h.logro = r.logro().trim();
        h.fechaPlan = fechaPlan;
        h.fechaReal = r.fechaReal();
        h.estado = r.estado();
        h.evidencia = blanco(r.evidencia());
        h.indicador = blanco(r.indicador());
        h.criterioAceptacion = blanco(r.criterioAceptacion());
        h.validador = blanco(r.validador());
        h.accionSiNoCumple = blanco(r.accionSiNoCumple());
        h.responsableId = r.responsableId();
    }

    private void guardarTareas(Long hitoId, List<Tarea> actividades) {
        vinculos.deleteByHitoId(hitoId);
        vinculos.flush();
        for (Tarea t : actividades) {
            HitoTarea v = new HitoTarea();
            v.hitoId = hitoId;
            v.tareaId = t.id;
            vinculos.save(v);
        }
    }

    private List<HitoResponse> responder(Proyecto p, List<Hito> hitos) {
        if (hitos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = hitos.stream().map(h -> h.id).toList();
        Map<Long, List<Long>> porHito = new HashMap<>();
        for (HitoTarea v : vinculos.findByHitoIdIn(ids)) {
            porHito.computeIfAbsent(v.hitoId, k -> new ArrayList<>()).add(v.tareaId);
        }
        Map<Long, Tarea> tareasPorId = new HashMap<>();
        for (Tarea t : tareas.findAllById(porHito.values().stream().flatMap(List::stream).distinct().toList())) {
            tareasPorId.put(t.id, t);
        }
        Calendario calendario = p.fechaInicio == null ? null : calendarios.de(p);
        return hitos.stream().map(h -> {
            List<ActividadHitoResponse> actividades = porHito.getOrDefault(h.id, List.of()).stream()
                .map(tareasPorId::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(t -> t.codigo))
                .map(ActividadHitoResponse::de)
                .toList();
            int semana = calendario == null || h.fechaPlan == null
                ? 0
                : SemanaHito.semanaDe(p.fechaInicio, calendario, h.fechaPlan);
            return new HitoResponse(
                h.id, h.proyectoId, h.codigo, h.logro, semana, h.fechaPlan, h.fechaBase, h.fechaReal,
                h.estado, h.evidencia, h.indicador, h.criterioAceptacion, h.validador, h.accionSiNoCumple,
                h.responsableId, actividades.stream().map(ActividadHitoResponse::id).toList(), actividades);
        }).toList();
    }

    private static String blanco(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private record Preparado(LocalDate fechaPlan, List<Tarea> actividades) {}
}
