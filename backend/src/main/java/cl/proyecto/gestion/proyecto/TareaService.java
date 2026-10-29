package cl.proyecto.gestion.proyecto;

import cl.proyecto.gestion.common.ApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issue A-05: EDT de 3 niveles con códigos automáticos (1, 1.1, 1.1.1).
 * Reglas: el tipo se deduce del padre; una subactividad no tiene hijos; un proyecto cerrado no se modifica;
 * al reordenar o eliminar, los hermanos y sus descendientes se renumeran sin duplicar códigos.
 */
@Service
public class TareaService {

    private final TareaRepository repo;
    private final ProyectoService proyectos;

    public TareaService(TareaRepository repo, ProyectoService proyectos) {
        this.repo = repo;
        this.proyectos = proyectos;
    }

    @Transactional(readOnly = true)
    public Tarea obtener(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la tarea " + id + "."));
    }

    /** Árbol completo de la EDT del proyecto, ordenado por posición. */
    @Transactional(readOnly = true)
    public List<NodoEdt> arbol(Long proyectoId) {
        proyectos.obtener(proyectoId);
        List<Tarea> raices = new ArrayList<>();
        Map<Long, List<Tarea>> hijos = agruparHijos(repo.findByProyectoId(proyectoId), raices);
        return construir(raices, hijos);
    }

    @Transactional
    public Tarea crear(Long proyectoId, TareaRequest r) {
        proyectos.obtenerEditable(proyectoId);
        Tarea padre = null;
        if (r.parentId() != null) {
            padre = obtener(r.parentId());
            if (!padre.proyectoId.equals(proyectoId)) {
                throw ApiException.invalido("PADRE_DE_OTRO_PROYECTO", "La tarea padre pertenece a otro proyecto.");
            }
            if (padre.tipo == TipoTarea.SUBACTIVIDAD) {
                throw ApiException.conflicto("JERARQUIA_INVALIDA",
                    "Una subactividad no puede tener hijos: la EDT llega hasta 3 niveles (etapa, actividad, subactividad).");
            }
        }
        Tarea t = new Tarea();
        t.proyectoId = proyectoId;
        t.parentId = padre == null ? null : padre.id;
        t.tipo = padre == null ? TipoTarea.ETAPA
            : padre.tipo == TipoTarea.ETAPA ? TipoTarea.ACTIVIDAD : TipoTarea.SUBACTIVIDAD;
        t.orden = hermanos(proyectoId, t.parentId).stream().mapToInt(h -> h.orden).max().orElse(0) + 1;
        t.codigo = padre == null ? String.valueOf(t.orden) : padre.codigo + "." + t.orden;
        copiar(t, r);
        return repo.save(t);
    }

    @Transactional
    public Tarea actualizar(Long id, TareaRequest r) {
        Tarea t = obtener(id);
        proyectos.obtenerEditable(t.proyectoId);
        copiar(t, r);
        return repo.save(t);
    }

    /** Elimina la tarea y todo lo que cuelga de ella, y renumera a sus hermanos. */
    @Transactional
    public void eliminar(Long id) {
        Tarea t = obtener(id);
        proyectos.obtenerEditable(t.proyectoId);
        Long proyectoId = t.proyectoId;
        Long parentId = t.parentId;
        try {
            repo.delete(t);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflicto("TAREA_REFERENCIADA",
                "No se puede eliminar: la tarea o alguna de sus subtareas está referenciada por otro registro (por ejemplo una solicitud de cambio).");
        }
        renumerar(proyectoId, parentId, hermanos(proyectoId, parentId));
    }

    /** Mueve la tarea a la posición indicada (1 = primera) entre sus hermanos y renumera. */
    @Transactional
    public void mover(Long id, int posicion) {
        Tarea t = obtener(id);
        proyectos.obtenerEditable(t.proyectoId);
        List<Tarea> lista = new ArrayList<>(hermanos(t.proyectoId, t.parentId));
        if (posicion < 1 || posicion > lista.size()) {
            throw ApiException.invalido("POSICION_INVALIDA",
                "La posición debe estar entre 1 y " + lista.size() + ".");
        }
        lista.removeIf(h -> h.id.equals(t.id));
        lista.add(posicion - 1, t);
        renumerar(t.proyectoId, t.parentId, lista);
    }

    // ---------------------------------------------------------------- utilidades

    private List<Tarea> hermanos(Long proyectoId, Long parentId) {
        return parentId == null
            ? repo.findByProyectoIdAndParentIdIsNullOrderByOrden(proyectoId)
            : repo.findByParentIdOrderByOrden(parentId);
    }

    private void copiar(Tarea t, TareaRequest r) {
        t.nombre = r.nombre().trim();
        t.descripcion = r.descripcion();
        t.responsableId = r.responsableId();
        if (r.duracionDias() != null) {
            t.duracionDias = r.duracionDias();
        } else if (t.duracionDias == null) {
            t.duracionDias = 1;
        }
        if (r.avance() != null) {
            t.avance = r.avance();
        } else if (t.avance == null) {
            t.avance = BigDecimal.ZERO;
        }
    }

    /** Agrupa por padre (cada lista ordenada por posición). Las etapas (sin padre) se devuelven en "raices". */
    private Map<Long, List<Tarea>> agruparHijos(List<Tarea> todas, List<Tarea> raices) {
        Map<Long, List<Tarea>> hijos = new HashMap<>();
        for (Tarea t : todas) {
            if (t.parentId == null) {
                raices.add(t);
            } else {
                hijos.computeIfAbsent(t.parentId, k -> new ArrayList<>()).add(t);
            }
        }
        raices.sort(Comparator.comparingInt(x -> x.orden));
        hijos.values().forEach(l -> l.sort(Comparator.comparingInt(x -> x.orden)));
        return hijos;
    }

    private List<NodoEdt> construir(List<Tarea> lista, Map<Long, List<Tarea>> hijos) {
        List<NodoEdt> nodos = new ArrayList<>();
        for (Tarea t : lista) {
            nodos.add(NodoEdt.de(t, construir(hijos.getOrDefault(t.id, List.of()), hijos)));
        }
        return nodos;
    }

    /**
     * Asigna posición 1..n a los hermanos (en el orden dado) y recalcula el código de cada uno y de todos sus
     * descendientes. Como (proyecto, código) es único, se hace en dos fases para no chocar a mitad de camino:
     * primero códigos temporales y después los definitivos.
     */
    private void renumerar(Long proyectoId, Long parentId, List<Tarea> ordenados) {
        String prefijo = parentId == null ? "" : obtener(parentId).codigo + ".";
        Map<Long, List<Tarea>> hijos = agruparHijos(repo.findByProyectoId(proyectoId), new ArrayList<>());
        List<Tarea> afectadas = new ArrayList<>();
        Map<Long, String> codigos = new HashMap<>();
        int posicion = 1;
        for (Tarea h : ordenados) {
            h.orden = posicion;
            calcular(h, prefijo + posicion, hijos, afectadas, codigos);
            posicion++;
        }
        List<Tarea> cambiadas = afectadas.stream().filter(t -> !codigos.get(t.id).equals(t.codigo)).toList();
        for (Tarea t : cambiadas) {
            t.codigo = "~" + t.id;
        }
        repo.saveAll(cambiadas);
        repo.flush();
        for (Tarea t : cambiadas) {
            t.codigo = codigos.get(t.id);
        }
        repo.saveAll(cambiadas);
        repo.flush();
    }

    private void calcular(Tarea t, String codigo, Map<Long, List<Tarea>> hijos,
                          List<Tarea> afectadas, Map<Long, String> codigos) {
        afectadas.add(t);
        codigos.put(t.id, codigo);
        for (Tarea c : hijos.getOrDefault(t.id, List.of())) {
            calcular(c, codigo + "." + c.orden, hijos, afectadas, codigos);
        }
    }
}
