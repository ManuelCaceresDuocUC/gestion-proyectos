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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecursoServiceTest {

    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired AsignacionService asignaciones;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    @Test
    void creaTipoDeRecurso() {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        assertThat(tipo.id).isNotNull();
        assertThat(tipo.nombre).startsWith("Profesional-");
        assertThat(tipo.unidad).isEqualTo("hora");
    }

    @Test
    void rechazaTipoDuplicado() {
        String nombre = "Profesional-" + System.nanoTime();
        tipos.crear(new TipoRecursoRequest(nombre, "hora"));

        assertThatThrownBy(() -> tipos.crear(new TipoRecursoRequest(nombre, "hora")))
            .isInstanceOf(ApiException.class)
            .extracting("codigo")
            .isEqualTo("TIPO_RECURSO_DUPLICADO");
    }

    @Test
    void creaRecursoConTipoExistente() {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        Recurso recurso = recursos.crear(new RecursoRequest(
            tipo.id,
            "Ana Pérez",
            new BigDecimal("25000.00"),
            new BigDecimal("8.0"),
            true));

        assertThat(recurso.id).isNotNull();
        assertThat(recurso.tipoRecursoId).isEqualTo(tipo.id);
        assertThat(recurso.tarifa).isEqualByComparingTo("25000.00");
        assertThat(recurso.capacidadHorasDia).isEqualByComparingTo("8.0");
        assertThat(recurso.activo).isTrue();
    }

    @Test
    void rechazaRecursoConTipoInexistente() {
        assertThatThrownBy(() -> recursos.crear(new RecursoRequest(
            -1L,
            "Ana Pérez",
            new BigDecimal("25000.00"),
            new BigDecimal("8.0"),
            true)))
            .isInstanceOf(ApiException.class)
            .extracting("codigo")
            .isEqualTo("TIPO_RECURSO_INEXISTENTE");
    }

    @Test
    void listaSoloRecursosActivos() {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        recursos.crear(new RecursoRequest(
            tipo.id, "Activo", BigDecimal.ONE, BigDecimal.ONE, true));

        recursos.crear(new RecursoRequest(
            tipo.id, "Inactivo", BigDecimal.ONE, BigDecimal.ONE, false));

        assertThat(recursos.listar(true))
            .extracting(recurso -> recurso.nombre)
            .contains("Activo")
            .doesNotContain("Inactivo");
    }

    @Test
    void desactivaRecursoConAsignacionesEnVezDeEliminarlo() {
        var tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));
        var recurso = recursos.crear(new RecursoRequest(
            tipo.id,
            "Asignado-" + System.nanoTime(),
            new BigDecimal("25000.00"),
            new BigDecimal("8.0"),
            true));

        var proyecto = proyectos.crear(new ProyectoRequest(
            "T-B05-" + System.nanoTime(),
            "Proyecto B-05",
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
        Tarea hoja = tareas.crear(
            proyecto.id,
            new TareaRequest(etapa.id, "Hoja", null, 2, null, null));

        asignaciones.crear(new AsignacionRequest(
            hoja.id,
            recurso.id,
            BigDecimal.ONE,
            null,
            null,
            null));

        recursos.eliminar(recurso.id);

        assertThat(recursos.obtener(recurso.id).activo).isFalse();
        assertThat(asignaciones.listarPorTarea(hoja.id))
            .extracting(asignacion -> asignacion.recursoId)
            .containsExactly(recurso.id);
    }
}