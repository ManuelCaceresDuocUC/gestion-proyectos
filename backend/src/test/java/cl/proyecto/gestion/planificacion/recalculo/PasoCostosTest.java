package cl.proyecto.gestion.planificacion.recalculo;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PasoCostosTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired AsignacionService asignaciones;
    @Autowired AsignacionRepository asignacionRepository;
    @Autowired RecalculoService recalculo;

    Long proyectoId;
    Long hojaId;
    Long actividadId;
    Long etapaId;

    @BeforeEach
    void escenario() {
        var proyecto = proyectos.crear(new ProyectoRequest(
            "T-B08-" + System.nanoTime(),
            "Proyecto costos",
            null,
            null,
            null,
            new BigDecimal("2000.00"),
            null,
            null,
            LocalDate.of(2026, 11, 5)));

        Tarea etapa = tareas.crear(
            proyecto.id,
            new TareaRequest(null, "Etapa", null, null, null, null));

        Tarea actividad = tareas.crear(
            proyecto.id,
            new TareaRequest(etapa.id, "Actividad", null, null, null, null));

        Tarea hoja = tareas.crear(
            proyecto.id,
            new TareaRequest(actividad.id, "Hoja", null, 2, null, null));

        etapaId = etapa.id;
        actividadId = actividad.id;
        hojaId = hoja.id;
        proyectoId = proyecto.id;

        var tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        var recurso = recursos.crear(new RecursoRequest(
            tipo.id,
            "Profesional-" + System.nanoTime(),
            new BigDecimal("100.00"),
            new BigDecimal("8.0"),
            true));

        asignaciones.crear(new AsignacionRequest(
            hojaId,
            recurso.id,
            new BigDecimal("15.00"),
            null,
            null,
            null));

        var segundoRecurso = recursos.crear(new RecursoRequest(
            tipo.id,
            "Profesional-" + System.nanoTime(),
            new BigDecimal("50.00"),
            new BigDecimal("8.0"),
            true));

        asignaciones.crear(new AsignacionRequest(
            hojaId,
            segundoRecurso.id,
            new BigDecimal("10.00"),
            null,
            null,
            null));
    }

    @Test
    void calculaConsolidaYEsIdempotente() {
        Asignacion asignacion = asignacionRepository
            .findByTareaIdOrderById(hojaId)
            .get(0);

        asignacion.cantidadReal = new BigDecimal("4.00");
        asignacionRepository.save(asignacion);

        Asignacion segundaAsignacion = asignacionRepository
            .findByTareaIdOrderById(hojaId)
            .get(1);
        segundaAsignacion.cantidadReal = new BigDecimal("6.00");
        asignacionRepository.save(segundaAsignacion);

        recalculo.recalcular(proyectoId);
        recalculo.recalcular(proyectoId);

        assertThat(asignacionRepository.findById(asignacion.id).orElseThrow().costoPlan)
            .isEqualByComparingTo("1500.00");

        assertThat(asignacionRepository.findById(asignacion.id).orElseThrow().costoReal)
            .isEqualByComparingTo("400.00");

        assertThat(asignacionRepository.findById(segundaAsignacion.id).orElseThrow().costoPlan)
            .isEqualByComparingTo("500.00");

        assertThat(asignacionRepository.findById(segundaAsignacion.id).orElseThrow().costoReal)
            .isEqualByComparingTo("300.00");

        assertThat(tareas.obtener(hojaId).costoPlan)
            .isEqualByComparingTo("2000.00");

        assertThat(tareas.obtener(hojaId).costoReal)
            .isEqualByComparingTo("700.00");

        assertThat(tareas.obtener(actividadId).costoPlan)
            .isEqualByComparingTo("2000.00");

        assertThat(tareas.obtener(actividadId).costoReal)
            .isEqualByComparingTo("700.00");

        assertThat(tareas.obtener(etapaId).costoPlan)
            .isEqualByComparingTo("2000.00");

        assertThat(tareas.obtener(etapaId).costoReal)
            .isEqualByComparingTo("700.00");

        assertThat(proyectos.obtener(proyectoId).costoPlan)
            .isEqualByComparingTo("2000.00");

        assertThat(proyectos.obtener(proyectoId).costoPlan)
            .isLessThanOrEqualTo(new BigDecimal("2000.00"));

        assertThat(proyectos.obtener(proyectoId).costoReal)
            .isEqualByComparingTo("700.00");
    }
}