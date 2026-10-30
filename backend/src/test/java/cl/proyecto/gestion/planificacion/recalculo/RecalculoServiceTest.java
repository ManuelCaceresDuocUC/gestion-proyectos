package cl.proyecto.gestion.planificacion.recalculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.calendario.NoLaborable;
import cl.proyecto.gestion.planificacion.calendario.NoLaborableRepository;
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

/** A-12. Integración con la base real: cambiar una duración mueve a las sucesoras y consolida hacia arriba. */
@SpringBootTest
@Transactional
class RecalculoServiceTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired DependenciaRepository dependencias;
    @Autowired RecalculoService recalculo;
    @Autowired NoLaborableRepository noLaborables;

    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    Long proyectoId;
    Long etapaId;
    Tarea a;
    Tarea b;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-A12-" + System.nanoTime() % 1_000_000, "Recalculo",
            null, null, null, BigDecimal.ZERO, null, null, d(2))).id;
        etapaId = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null)).id;
        a = tareas.crear(proyectoId, new TareaRequest(etapaId, "A", null, 3, null, null));
        b = tareas.crear(proyectoId, new TareaRequest(etapaId, "B", null, 2, null, null));
        Dependencia dep = new Dependencia();
        dep.predecesoraId = a.id;
        dep.sucesoraId = b.id;
        dep.tipo = TipoDependencia.FS;
        dependencias.save(dep);
        recalculo.recalcular(proyectoId);
    }

    @Test
    void calculaFechasConDependencias() {
        assertThat(tareas.obtener(a.id).inicioPlan).isEqualTo(d(2));
        assertThat(tareas.obtener(a.id).finPlan).isEqualTo(d(4));
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(5));
        assertThat(tareas.obtener(b.id).finPlan).isEqualTo(d(6));
    }

    @Test
    void cambiarUnaDuracionMueveALaSucesoraYConsolida() {
        tareas.actualizar(a.id, new TareaRequest(null, "A", null, 6, null, null)); // recalcula solo
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(10));
        assertThat(tareas.obtener(b.id).finPlan).isEqualTo(d(11));
        Tarea etapa = tareas.obtener(etapaId);
        assertThat(etapa.inicioPlan).isEqualTo(d(2));
        assertThat(etapa.finPlan).isEqualTo(d(11));
        assertThat(proyectos.obtener(proyectoId).fechaFinPlan).isEqualTo(d(11));
    }

    @Test
    void elAvanceDelPadreEsPromedioPonderadoPorDuracion() {
        tareas.actualizar(a.id, new TareaRequest(null, "A", null, 6, null, new BigDecimal("50")));
        assertThat(tareas.obtener(etapaId).avance).isEqualByComparingTo("37.50"); // (50*6 + 0*2) / 8
        assertThat(proyectos.obtener(proyectoId).avance).isEqualByComparingTo("37.50");
    }

    @Test
    void cambiarLaFechaDeInicioDelProyectoMueveTodo() {
        proyectos.actualizar(proyectoId, new ProyectoRequest(proyectos.obtener(proyectoId).codigo, "Recalculo",
            null, null, null, BigDecimal.ZERO, null, null, d(9)));
        assertThat(tareas.obtener(a.id).inicioPlan).isEqualTo(d(9));
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(12));
    }

    @Test
    void unFeriadoCorreLasFechas() {
        NoLaborable feriado = new NoLaborable();
        feriado.proyectoId = proyectoId;
        feriado.fecha = d(3); // martes
        feriado.descripcion = "Feriado de prueba";
        noLaborables.save(feriado);
        recalculo.recalcular(proyectoId);
        assertThat(tareas.obtener(a.id).finPlan).isEqualTo(d(5));    // lun, mie, jue
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(6)); // vie
        assertThat(tareas.obtener(b.id).finPlan).isEqualTo(d(9));    // vie, lun
    }

    @Test
    void unFeriadoGeneralAfectaATodosLosProyectos() {
        NoLaborable feriado = new NoLaborable();
        feriado.proyectoId = null;
        feriado.fecha = d(3);
        noLaborables.save(feriado);
        recalculo.recalcular(proyectoId);
        assertThat(tareas.obtener(a.id).finPlan).isEqualTo(d(5));
    }

    @Test
    void unCicloRevierteConUnErrorLegible() {
        Dependencia vuelta = new Dependencia();
        vuelta.predecesoraId = b.id;
        vuelta.sucesoraId = a.id;
        vuelta.tipo = TipoDependencia.FS;
        dependencias.save(vuelta);
        assertThatThrownBy(() -> recalculo.recalcular(proyectoId))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("CICLO_EN_DEPENDENCIAS");
    }
}
