package cl.proyecto.gestion.planificacion.lineabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.dependencia.Dependencia;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaRepository;
import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
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

/** A-14. Base real; cada test se revierte. Proyecto desde el lunes 2-nov-2026; A (3 dias) -FS-> B (2 dias). */
@SpringBootTest
@Transactional
class LineaBaseServiceTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired DependenciaRepository dependencias;
    @Autowired LineaBaseService lineaBase;

    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    Long proyectoId;
    Tarea etapa;
    Tarea a;
    Tarea b;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-A14-" + System.nanoTime() % 1_000_000, "Base", null,
            null, null, BigDecimal.ZERO, null, null, d(2))).id;
        etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null));
        a = tareas.crear(proyectoId, new TareaRequest(etapa.id, "A", null, 3, null, null));
        b = tareas.crear(proyectoId, new TareaRequest(etapa.id, "B", null, 2, null, null));
        Dependencia dep = new Dependencia();
        dep.predecesoraId = a.id;
        dep.sucesoraId = b.id;
        dep.tipo = TipoDependencia.FS;
        dependencias.save(dep);
    }

    @Test
    void fijarCopiaElPlanALaBaseYCreaLaVersion1() {
        LineaBase lb = lineaBase.fijar(proyectoId, "Linea base inicial");
        assertThat(lb.version).isEqualTo(1);
        assertThat(lb.motivo).isEqualTo("Linea base inicial");
        Tarea tb = tareas.obtener(b.id);
        assertThat(tb.inicioBase).isEqualTo(d(5));
        assertThat(tb.finBase).isEqualTo(d(6));
        assertThat(tb.inicioPlan).isEqualTo(tb.inicioBase);
        assertThat(tareas.obtener(etapa.id).finBase).isEqualTo(d(6));
        assertThat(proyectos.obtener(proyectoId).fechaFinBase).isEqualTo(d(6));
        assertThat(lb.fechaFin).isEqualTo(d(6));
    }

    @Test
    void loCambiadoDespuesModificaElPlanPeroNoLaBase() {
        lineaBase.fijar(proyectoId, null);
        tareas.actualizar(a.id, new TareaRequest(null, "A", null, 6, null, null)); // recalcula solo
        Tarea tb = tareas.obtener(b.id);
        assertThat(tb.inicioPlan).isEqualTo(d(10));
        assertThat(tb.inicioBase).isEqualTo(d(5)); // la base sigue igual

        ComparacionPlan c = lineaBase.comparar(proyectoId, d(2));
        FilaComparacion fb = c.tareas().stream().filter(f -> f.codigo().equals("1.2")).findFirst().orElseThrow();
        assertThat(fb.desviacionInicioDias()).isEqualTo(3); // del jue 5 al mar 10: vie 6, lun 9, mar 10
        assertThat(c.resumen().desviacionFinDias()).isEqualTo(3);
        assertThat(c.versionLineaBase()).isEqualTo(1);
    }

    @Test
    void unaSegundaLineaBaseCreaLaVersion2YReemplazaLosValoresBase() {
        lineaBase.fijar(proyectoId, null);
        tareas.actualizar(a.id, new TareaRequest(null, "A", null, 6, null, null));
        LineaBase v2 = lineaBase.fijar(proyectoId, "Tras aprobar el cambio");
        assertThat(v2.version).isEqualTo(2);
        assertThat(tareas.obtener(b.id).inicioBase).isEqualTo(d(10));
        assertThat(lineaBase.listar(proyectoId)).extracting(l -> l.version).containsExactly(2, 1);
    }

    @Test
    void sinFechaDeInicioNoSePuedeFijar() {
        Long sinInicio = proyectos.crear(new ProyectoRequest("T-A14-S" + System.nanoTime() % 1_000_000, "Sin inicio",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
        tareas.crear(sinInicio, new TareaRequest(null, "E", null, null, null, null));
        assertThatThrownBy(() -> lineaBase.fijar(sinInicio, null))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("SIN_PLAN");
    }

    @Test
    void unProyectoTerminadoNoAceptaUnaNuevaBase() {
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> lineaBase.fijar(proyectoId, null))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }

    @Test
    void compararSinBaseDevuelveLasTareasSinDesviaciones() {
        ComparacionPlan c = lineaBase.comparar(proyectoId, d(2));
        assertThat(c.versionLineaBase()).isNull();
        assertThat(c.tareas()).hasSize(3);
        assertThat(c.tareas()).allMatch(f -> f.desviacionFinDias() == null);
    }
}
