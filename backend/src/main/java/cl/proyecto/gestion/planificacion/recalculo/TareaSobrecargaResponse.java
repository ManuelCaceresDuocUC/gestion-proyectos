package cl.proyecto.gestion.planificacion.recalculo;

public record TareaSobrecargaResponse(Long id, String codigo, String nombre) {
    static TareaSobrecargaResponse de(SobrecargaAlertaTarea tarea) {
        return new TareaSobrecargaResponse(tarea.tareaId, tarea.tareaCodigo, tarea.tareaNombre);
    }
}
