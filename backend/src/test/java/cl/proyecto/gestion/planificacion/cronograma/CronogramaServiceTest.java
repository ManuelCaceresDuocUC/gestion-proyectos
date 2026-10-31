package cl.proyecto.gestion.planificacion.cronograma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaRequest;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaService;
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

/** A-13 (PATCH de tareas). El proyecto parte el lunes 2-nov-2026; A (3 dias) -FS-> B (2 dias). */
@SpringBootTest
@Transactional
class CronogramaServiceTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired DependenciaService dependencias;
    @Autowired CronogramaService cronograma;

    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    Long proyectoId;
    Tarea etapa;
    Tarea a;
    Tarea b;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-A13-" + System.nanoTime() % 1_000_000, "Patch", null,
            null, null, BigDecimal.ZERO, null, null, d(2))).id;
        etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null));
        a = tareas.crear(proyectoId, new TareaRequest(etapa.id, "A", null, 3, null, null));
        b = tareas.crear(proyectoId, new TareaRequest(etapa.id, "B", null, 2, null, null));
        dependencias.crear(proyectoId, new DependenciaRequest(a.id, b.id, TipoDependencia.FS, 0));
    }

    @Test
    void cambiarLaDuracionMueveALasSucesorasEInformaCuales() {
        var r = cronograma.parchear(a.id, new TareaPatch(6, null, null, null));
        assertThat(r.tarea().duracionDias()).isEqualTo(6);
        assertThat(r.tarea().finPlan()).isEqualTo(d(9));
        var deB = r.cambios().stream().filter(x -> x.codigo().equals("1.2")).findFirst().orElseThrow();
        assertThat(deB.inicioAntes()).isEqualTo(d(5));
        assertThat(deB.inicioDespues()).isEqualTo(d(10));
        assertThat(deB.finDespues()).isEqualTo(d(11));
    }

    @Test
    void registraFechasRealesYAvance() {
        var r = cronograma.parchear(a.id, new TareaPatch(null, d(2), d(5), new BigDecimal("100")));
        assertThat(r.tarea().inicioReal()).isEqualTo(d(2));
        assertThat(r.tarea().finReal()).isEqualTo(d(5));
        assertThat(r.tarea().avance()).isEqualByComparingTo("100");
        assertThat(r.cambios()).isEmpty(); // no movio fechas plan
        assertThat(tareas.obtener(etapa.id).avance).isEqualByComparingTo("60.00"); // (100*3 + 0*2) / 5
    }

    @Test
    void unPatchVacioSeRechaza() {
        assertThatThrownBy(() -> cronograma.parchear(a.id, new TareaPatch(null, null, null, null)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PATCH_VACIO");
    }

    @Test
    void unaTareaConHijosNoSeEditaAMano() {
        assertThatThrownBy(() -> cronograma.parchear(etapa.id, new TareaPatch(10, null, null, null)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TAREA_CON_HIJOS");
    }

    @Test
    void laFechaRealDeTerminoNoPuedeSerAnteriorAlInicio() {
        assertThatThrownBy(() -> cronograma.parchear(a.id, new TareaPatch(null, d(5), d(3), null)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("FECHAS_REALES_INVALIDAS");
    }

    @Test
    void unProyectoTerminadoNoSeModifica() {
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> cronograma.parchear(a.id, new TareaPatch(5, null, null, null)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }
}
