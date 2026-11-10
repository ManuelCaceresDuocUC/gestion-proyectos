package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.hito.Hito;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaResponse;
import cl.proyecto.gestion.planificacion.recalculo.TareaSobrecargaResponse;
import cl.proyecto.gestion.proyecto.Tarea;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recorta las listas del tablero. No recalcula ni toca KPI.
 * Un filtro nulo no recorta.
 */
public final class FiltroTablero {

    private FiltroTablero() {
    }

    public static List<SolicitudCambio> solicitudes(
            List<SolicitudCambio> lista,
            EstadoSolicitud estado,
            PrioridadSolicitud prioridad,
            RiesgoSolicitud riesgo,
            Long responsableId) {
        return lista.stream()
                .filter(s -> estado == null || s.estado == estado)
                .filter(s -> prioridad == null || s.prioridad == prioridad)
                .filter(s -> riesgo == null || s.riesgo == riesgo)
                .filter(s -> responsableId == null || responsableId.equals(s.responsableId))
                .toList();
    }

    /** {@code raizId} y todos sus descendientes en la EDT. Vacío si la raíz no está. */
    public static Set<Long> rama(Long raizId, List<Tarea> edt) {
        if (raizId == null || edt == null) {
            return Set.of();
        }
        Map<Long, List<Long>> hijos = new HashMap<>();
        boolean hayRaiz = false;
        for (Tarea tarea : edt) {
            if (raizId.equals(tarea.id)) {
                hayRaiz = true;
            }
            if (tarea.parentId != null) {
                hijos.computeIfAbsent(tarea.parentId, k -> new ArrayList<>()).add(tarea.id);
            }
        }
        if (!hayRaiz) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        ArrayDeque<Long> cola = new ArrayDeque<>();
        cola.add(raizId);
        while (!cola.isEmpty()) {
            Long id = cola.removeFirst();
            if (ids.add(id)) {
                List<Long> siguientes = hijos.get(id);
                if (siguientes != null) {
                    cola.addAll(siguientes);
                }
            }
        }
        return Set.copyOf(ids);
    }

    public static boolean fechaEnRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return true;
        }
        if (fecha == null) {
            return false;
        }
        if (desde != null && fecha.isBefore(desde)) {
            return false;
        }
        if (hasta != null && fecha.isAfter(hasta)) {
            return false;
        }
        return true;
    }

    public static List<Hito> hitos(
            List<Hito> lista,
            Set<Long> rama,
            LocalDate desde,
            LocalDate hasta,
            Long responsableId,
            Map<Long, List<Long>> tareasPorHito) {
        return lista.stream()
                .filter(h -> responsableId == null || responsableId.equals(h.responsableId))
                .filter(h -> fechaEnRango(h.fechaPlan, desde, hasta))
                .filter(h -> rama == null || cruza(tareasPorHito.getOrDefault(h.id, List.of()), rama))
                .toList();
    }

    public static boolean tareaPasa(Tarea tarea, Set<Long> rama, LocalDate desde, LocalDate hasta, Long responsableId) {
        if (rama != null && !rama.contains(tarea.id)) {
            return false;
        }
        if (responsableId != null && !responsableId.equals(tarea.responsableId)) {
            return false;
        }
        return fechaEnRango(tarea.finPlan, desde, hasta);
    }

    public static List<SobrecargaAlertaResponse> alertas(
            List<SobrecargaAlertaResponse> lista,
            Set<Long> rama,
            LocalDate desde,
            LocalDate hasta,
            Long responsableId) {
        return lista.stream()
                .filter(a -> responsableId == null || responsableId.equals(a.recursoId()))
                .filter(a -> fechaEnRango(a.fecha(), desde, hasta))
                .filter(a -> rama == null || a.tareas().stream().map(TareaSobrecargaResponse::id).anyMatch(rama::contains))
                .toList();
    }

    private static boolean cruza(List<Long> ids, Set<Long> rama) {
        for (Long id : ids) {
            if (rama.contains(id)) {
                return true;
            }
        }
        return false;
    }
}
