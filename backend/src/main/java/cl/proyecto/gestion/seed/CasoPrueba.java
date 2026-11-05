package cl.proyecto.gestion.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * C-07: forma de {@code seed/caso-prueba.json}. Solo el recorte v1 (proyecto, tipos, profesionales,
 * recursos y EDT). El resto del archivo (usuarios, dependencias, hitos, solicitudes) se ignora.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CasoPrueba(
    CasoProyecto proyecto,
    List<CasoTipoRecurso> tiposRecurso,
    List<CasoProfesional> profesionales,
    List<CasoRecurso> recursos,
    List<CasoEtapa> estructura
) {
    public List<CasoActividad> actividades() {
        return estructura().stream().flatMap(e -> e.actividades().stream()).toList();
    }

    public List<CasoSubactividad> subactividades() {
        return actividades().stream().flatMap(a -> a.subactividades().stream()).toList();
    }

    @Override
    public List<CasoTipoRecurso> tiposRecurso() {
        return tiposRecurso == null ? List.of() : tiposRecurso;
    }

    @Override
    public List<CasoProfesional> profesionales() {
        return profesionales == null ? List.of() : profesionales;
    }

    @Override
    public List<CasoRecurso> recursos() {
        return recursos == null ? List.of() : recursos;
    }

    @Override
    public List<CasoEtapa> estructura() {
        return estructura == null ? List.of() : estructura;
    }
}

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
    Integer avance
) {
    @Override
    public List<CasoSubactividad> subactividades() {
        return subactividades == null ? List.of() : subactividades;
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
    Integer avance
) {}
