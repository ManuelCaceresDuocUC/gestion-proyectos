package cl.proyecto.gestion.recurso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AsignacionServiceTest {

    @Autowired AsignacionService asignaciones;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    Long tareaHojaId;
    Long tareaPadreId;
    Long recursoId;
    BigDecimal tarifa = new BigDecimal("25000.00");

    @BeforeEach
    void escenario() {
        var proyecto = proyectos.crear(new ProyectoRequest(
            "T-B06-" + System.nanoTime(),
            "Proyecto B-06",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            null,
            LocalDate.of(2026, 11, 5)));

        Tarea etapa = tareas.crear(
            proyecto.id,
            new TareaRequest(null, "Etapa", null, null, null, null));

        Tarea actividad = tareas.crear(
            proyecto.id,
            new TareaRequest(etapa.id, "Actividad", null, null, null, null));

        Tarea subactividad = tareas.crear(
            proyecto.id,
            new TareaRequest(actividad.id, "Subactividad", null, 2, null, null));

        tareaPadreId = actividad.id;
        tareaHojaId = subactividad.id;

        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        recursoId = recursos.crear(new RecursoRequest(
            tipo.id,
            "Ana Pérez-" + System.nanoTime(),
            tarifa,
            new BigDecimal("8.0"),
            true)).id;
    }

    private AsignacionRequest request() {
        return new AsignacionRequest(
            tareaHojaId,
            recursoId,
            new BigDecimal("1.00"),
            null,
            new BigDecimal("40.0"),
            new BigDecimal("2.0"));
    }

    @Test
    void creaAsignacionUsandoTarifaVigenteDelRecurso() {
        Asignacion asignacion = asignaciones.crear(request());

        assertThat(asignacion.id).isNotNull();
        assertThat(asignacion.tareaId).isEqualTo(tareaHojaId);
        assertThat(asignacion.recursoId).isEqualTo(recursoId);
        assertThat(asignacion.tarifa).isEqualByComparingTo(tarifa);
        assertThat(asignacion.horasSemana).isEqualByComparingTo("40.0");
        assertThat(asignacion.semanas).isEqualByComparingTo("2.0");
    }

    @Test
    void rechazaAsignacionDuplicada() {
        asignaciones.crear(request());

        assertThatThrownBy(() -> asignaciones.crear(request()))
            .isInstanceOf(ApiException.class)
            .extracting("codigo")
            .isEqualTo("ASIGNACION_DUPLICADA");
    }

    @Test
    void rechazaAsignacionEnTareaConHijos() {
        AsignacionRequest request = new AsignacionRequest(
            tareaPadreId,
            recursoId,
            BigDecimal.ONE,
            tarifa,
            null,
            null);

        assertThatThrownBy(() -> asignaciones.crear(request))
            .isInstanceOf(ApiException.class)
            .extracting("codigo")
            .isEqualTo("TAREA_NO_HOJA");
    }

    @Test
    void listaAsignacionesDeUnaTarea() {
        asignaciones.crear(request());

        assertThat(asignaciones.listarPorTarea(tareaHojaId))
            .singleElement()
            .satisfies(asignacion -> {
                assertThat(asignacion.tareaId).isEqualTo(tareaHojaId);
                assertThat(asignacion.recursoId).isEqualTo(recursoId);
            });
    }
}