package cl.proyecto.gestion.planificacion.recalculo;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecurso;
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
class PasoSobreasignacionTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired TareaRepository tareaRepo;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired AsignacionService asignaciones;
    @Autowired AsignacionRepository asignacionRepo;
    @Autowired SobrecargaAlertaService alertas;
    @Autowired SobrecargaAlertaRepository alertasRepo;
    @Autowired RecalculoService recalculo;
    @Autowired PasoSobreasignacion paso;

    Long proyectoId;
    Long etapaId;
    Long recursoId;
    Long tareaA;
    Long tareaB;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
            "T-B09-" + System.nanoTime(),
            "Sobreasignación",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            "LMXJV",
            LocalDate.of(2026, 11, 9))).id;

        Tarea etapa = tareas.crear(proyectoId,
            new TareaRequest(null, "Etapa", null, null, null, null));
        etapaId = etapa.id;
        tareaA = tareas.crear(proyectoId,
            new TareaRequest(etapa.id, "Tarea A", null, 2, null, null)).id;
        tareaB = tareas.crear(proyectoId,
            new TareaRequest(etapa.id, "Tarea B", null, 2, null, null)).id;

        TipoRecurso tipo = tipos.crear(new TipoRecursoRequest(
            "Profesional-B09-" + System.nanoTime(), "hora"));
        Recurso recurso = recursos.crear(new RecursoRequest(
            tipo.id, "Profesional B09", new BigDecimal("100"), new BigDecimal("8.0"), true));
        recursoId = recurso.id;
    }

    @Test
    void alertaPorSolapamientoIncluyeFechaYTareas() {
        asignaciones.crear(new AsignacionRequest(
            tareaA, recursoId, new BigDecimal("16"), null, null, null));
        asignaciones.crear(new AsignacionRequest(
            tareaB, recursoId, new BigDecimal("8"), null, null, null));

        var resultado = alertas.listar(proyectoId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(alerta -> {
            assertThat(alerta.cargaHoras()).isEqualByComparingTo("12.0000");
            assertThat(alerta.capacidadHorasDia()).isEqualByComparingTo("8.0");
            assertThat(alerta.excesoHoras()).isEqualByComparingTo("4.0000");
            assertThat(alerta.tareas()).extracting(TareaSobrecargaResponse::codigo)
                .containsExactly("1.1", "1.2");
        });
    }

    @Test
    void noAlertaCuandoCargaIgualaCapacidadNiUsaHorasSemana() {
        asignaciones.crear(new AsignacionRequest(
            tareaA, recursoId, new BigDecimal("16"), null, new BigDecimal("1"), new BigDecimal("99")));

        assertThat(alertas.listar(proyectoId)).isEmpty();
    }

    @Test
    void recalculoEsIdempotente() {
        asignaciones.crear(new AsignacionRequest(
            tareaA, recursoId, new BigDecimal("16"), null, null, null));
        asignaciones.crear(new AsignacionRequest(
            tareaB, recursoId, new BigDecimal("8"), null, null, null));

        var primera = alertas.listar(proyectoId);
        recalculo.recalcular(proyectoId);
        recalculo.recalcular(proyectoId);
        var segunda = alertas.listar(proyectoId);

        assertThat(segunda).hasSize(primera.size());
        assertThat(segunda).zipSatisfy(primera, (actual, esperado) -> {
            assertThat(actual.recursoId()).isEqualTo(esperado.recursoId());
            assertThat(actual.fecha()).isEqualTo(esperado.fecha());
            assertThat(actual.cargaHoras()).isEqualByComparingTo(esperado.cargaHoras());
            assertThat(actual.excesoHoras()).isEqualByComparingTo(esperado.excesoHoras());
        });
    }

    @Test
    void ignoraRecursoSinCapacidadDiaria() {
        TipoRecurso tipo = tipos.crear(new TipoRecursoRequest(
            "Equipamiento-B09-" + System.nanoTime(), "unidad"));
        Long equipoId = recursos.crear(new RecursoRequest(
            tipo.id, "Máquina", new BigDecimal("85000"), null, true)).id;

        asignaciones.crear(new AsignacionRequest(
            tareaA, equipoId, new BigDecimal("100"), null, null, null));

        assertThat(alertas.listar(proyectoId)).isEmpty();
    }

    @Test
    void ignoraAsignacionEnTareaPadre() {
        Asignacion asignacion = new Asignacion();
        asignacion.tareaId = etapaId;
        asignacion.recursoId = recursoId;
        asignacion.cantidad = new BigDecimal("80");
        asignacion.tarifa = new BigDecimal("100");
        asignacionRepo.saveAndFlush(asignacion);

        recalculo.recalcular(proyectoId);

        assertThat(alertas.listar(proyectoId)).isEmpty();
    }

    @Test
    void ignoraTareaDeDuracionCero() {
        Long hitoId = tareas.crear(proyectoId,
            new TareaRequest(etapaId, "Hito", null, 0, null, null)).id;
        asignaciones.crear(new AsignacionRequest(
            hitoId, recursoId, new BigDecimal("80"), null, null, null));

        assertThat(alertas.listar(proyectoId)).isEmpty();
    }

    @Test
    void ignoraTareaSinFechasPlan() {
        asignaciones.crear(new AsignacionRequest(
            tareaA, recursoId, new BigDecimal("20"), null, null, null));
        assertThat(alertas.listar(proyectoId)).isNotEmpty();

        Tarea hoja = tareaRepo.findById(tareaA).orElseThrow();
        hoja.inicioPlan = null;
        hoja.finPlan = null;
        tareaRepo.saveAndFlush(hoja);
        // Solo el paso: el motor de fechas volvería a rellenar inicio/fin.
        paso.ejecutar(proyectoId);

        assertThat(alertas.listar(proyectoId)).isEmpty();
        assertThat(alertasRepo.findByProyectoIdOrderByFechaAscRecursoNombreAsc(proyectoId)).isEmpty();
    }

    @Test
    void ignoraIntervaloSinDiasHabiles() {
        asignaciones.crear(new AsignacionRequest(
            tareaA, recursoId, new BigDecimal("20"), null, null, null));
        Tarea hoja = tareaRepo.findById(tareaA).orElseThrow();
        hoja.finPlan = hoja.inicioPlan.minusDays(1);
        tareaRepo.saveAndFlush(hoja);
        paso.ejecutar(proyectoId);

        assertThat(alertas.listar(proyectoId)).isEmpty();
    }
}
