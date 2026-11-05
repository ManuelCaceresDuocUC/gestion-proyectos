package cl.proyecto.gestion.recurso;

public record TipoRecursoResponse(
    Long id,
    String nombre,
    String unidad) {

    public static TipoRecursoResponse de(TipoRecurso tipo) {
        return new TipoRecursoResponse(tipo.id, tipo.nombre, tipo.unidad);
    }
}