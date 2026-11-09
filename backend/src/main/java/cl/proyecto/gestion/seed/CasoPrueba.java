package cl.proyecto.gestion.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Forma de {@code seed/caso-prueba.json}.
 * C-07: proyecto, tipos, profesionales, recursos, EDT y asignaciones.
 * C-11: dependencias y solicitudes. C-01: usuarios. Hitos y KPI se ignoran.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CasoPrueba(
    CasoProyecto proyecto,
    List<CasoUsuario> usuarios,
    List<CasoTipoRecurso> tiposRecurso,
    List<CasoProfesional> profesionales,
    List<CasoRecurso> recursos,
    List<CasoEtapa> estructura,
    List<CasoDependencia> dependencias,
    List<CasoSolicitud> solicitudesCambio
) {
    public CasoPrueba {
        usuarios = usuarios == null ? List.of() : usuarios;
        tiposRecurso = tiposRecurso == null ? List.of() : tiposRecurso;
        profesionales = profesionales == null ? List.of() : profesionales;
        recursos = recursos == null ? List.of() : recursos;
        estructura = estructura == null ? List.of() : estructura;
        dependencias = dependencias == null ? List.of() : dependencias;
        solicitudesCambio = solicitudesCambio == null ? List.of() : solicitudesCambio;
    }

    public List<CasoActividad> actividades() {
        return estructura().stream().flatMap(e -> e.actividades().stream()).toList();
    }

    public List<CasoSubactividad> subactividades() {
        return actividades().stream().flatMap(a -> a.subactividades().stream()).toList();
    }

    /** Asignaciones de hojas (actividad 1.4 y subactividades). */
    public List<CasoAsignacion> asignacionesDeHojas() {
        Stream<CasoAsignacion> deActividades = actividades().stream()
            .filter(CasoActividad::esHoja)
            .flatMap(a -> a.asignaciones().stream());
        Stream<CasoAsignacion> deSubs = subactividades().stream()
            .flatMap(s -> s.asignaciones().stream());
        return Stream.concat(deActividades, deSubs).toList();
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoUsuario(String nombre, String email, String password, String rol) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoProyecto(
    String codigo,
    String nombre,
    String descripcion,
    String estado,
    LocalDate fechaInicio,
    Integer duracionSemanas,
    String diasLaborales,
    BigDecimal presupuesto,
    String moneda,
    String responsable
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoTipoRecurso(String nombre, String unidad) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoProfesional(String clave, String nombre, BigDecimal tarifaHora, BigDecimal capacidadHorasDia) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoRecurso(String clave, String nombre, String tipo, String unidad, BigDecimal costoUnitario) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoEtapa(String codigo, String nombre, String objetivo, List<CasoActividad> actividades) {
    @Override
    public List<CasoActividad> actividades() {
        return actividades == null ? List.of() : actividades;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoActividad(
    String codigo,
    String nombre,
    List<CasoSubactividad> subactividades,
    Boolean hoja,
    Integer referenciaSemana,
    Integer duracionSemanas,
    Integer avance,
    List<CasoAsignacion> asignaciones
) {
    @Override
    public List<CasoSubactividad> subactividades() {
        return subactividades == null ? List.of() : subactividades;
    }

    @Override
    public List<CasoAsignacion> asignaciones() {
        return asignaciones == null ? List.of() : asignaciones;
    }

    /** Actividad sin hijos (ej. 1.4): lleva duración propia. */
    boolean esHoja() {
        return Boolean.TRUE.equals(hoja) || subactividades().isEmpty();
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoSubactividad(
    String codigo,
    String nombre,
    Integer referenciaSemana,
    Integer duracionSemanas,
    Integer avance,
    List<CasoAsignacion> asignaciones
) {
    @Override
    public List<CasoAsignacion> asignaciones() {
        return asignaciones == null ? List.of() : asignaciones;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoAsignacion(String recurso, BigDecimal cantidad) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoDependencia(String predecesora, String sucesora, String tipo, Integer desfaseDias) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoImpactoEstimado(Integer dias, BigDecimal costo) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record CasoSolicitud(
    String codigo,
    String nombre,
    String tipo,
    String prioridad,
    String riesgo,
    String impacto,
    String estado,
    String responsable,
    String justificacion,
    CasoImpactoEstimado impactoEstimado,
    String tareaAfectada
) {}
