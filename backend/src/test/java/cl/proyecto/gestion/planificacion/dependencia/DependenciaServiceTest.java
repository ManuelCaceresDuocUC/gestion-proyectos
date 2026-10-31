package cl.proyecto.gestion.planificacion.dependencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.motor.TipoDependencia;
import cl.proyecto.gestion.planificacion.recalculo.CambioFecha;
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

/** A-09 y A-13 (dependencias). Base real; cada test se revierte. El proyecto parte el lunes 2-nov-2026. */
@SpringBootTest
@Transactional
class DependenciaServiceTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired DependenciaService dependencias;

    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    Long proyectoId;
    Tarea etapa;
    Tarea a; // 1.1, 3 dias
    Tarea b; // 1.2, 2 dias
    Tarea c; // 1.3, 1 dia

    @BeforeEach
    void escenario() {
        proyectoId = nuevoProyecto("T-A09-");
        etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null));
        a = tareas.crear(proyectoId, new TareaRequest(etapa.id, "Analisis", null, 3, null, null));
        b = tareas.crear(proyectoId, new TareaRequest(etapa.id, "Diseno", null, 2, null, null));
        c = tareas.crear(proyectoId, new TareaRequest(etapa.id, "Construccion", null, 1, null, null));
    }

    Long nuevoProyecto(String prefijo) {
        return proyectos.crear(new ProyectoRequest(prefijo + System.nanoTime() % 1_000_000, "Dependencias", null,
            null, null, BigDecimal.ZERO, null, null, d(2))).id;
    }

    DependenciaRequest dep(Tarea x, Tarea y, TipoDependencia tipo, Integer lag) {
        return new DependenciaRequest(x.id, y.id, tipo, lag);
    }

    @Test
    void creaLaDependenciaYInformaLasFechasQueSeMovieron() {
        var r = dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, null));
        assertThat(r.dependencia().desfaseDias()).isZero();
        CambioFecha deB = r.cambios().stream().filter(x -> x.codigo().equals("1.2")).findFirst().orElseThrow();
        assertThat(deB.inicioAntes()).isEqualTo(d(2));
        assertThat(deB.inicioDespues()).isEqualTo(d(5));
        assertThat(r.cambios()).noneMatch(x -> x.codigo().equals("1.1")); // la predecesora no se movio
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(5));
    }

    @Test
    void rechazaUnCicloDirectoYExplicaCualEs() {
        dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0));
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(b, a, TipoDependencia.FS, 0)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("1.1 Analisis")
            .hasMessageContaining("1.2 Diseno")
            .extracting("codigo").isEqualTo("CICLO_EN_DEPENDENCIAS");
        assertThat(dependencias.listar(proyectoId)).hasSize(1); // no se guardo nada
    }

    @Test
    void rechazaUnCicloLargo() {
        dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0));
        dependencias.crear(proyectoId, dep(b, c, TipoDependencia.SS, 0));
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(c, a, TipoDependencia.FF, 0)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("1.3 Construccion → 1.1 Analisis → 1.2 Diseno → 1.3 Construccion")
            .extracting("codigo").isEqualTo("CICLO_EN_DEPENDENCIAS");
    }

    @Test
    void rechazaDependenciaConsigoMisma() {
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(a, a, TipoDependencia.FS, 0)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("DEPENDENCIA_A_SI_MISMA");
    }

    @Test
    void rechazaDependenciaDuplicada() {
        dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0));
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(a, b, TipoDependencia.SS, 1)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("DEPENDENCIA_DUPLICADA");
    }

    @Test
    void rechazaDependenciaSobreUnaTareaConSubtareas() {
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(etapa, a, TipoDependencia.FS, 0)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("DEPENDENCIA_EN_PADRE");
    }

    @Test
    void rechazaTareasDeOtroProyecto() {
        Long otro = nuevoProyecto("T-A09-O");
        Tarea ajena = tareas.crear(otro, new TareaRequest(null, "Ajena", null, null, null, null));
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(a, ajena, TipoDependencia.FS, 0)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TAREA_DE_OTRO_PROYECTO");
    }

    @Test
    void cambiarElTipoOElDesfaseRecalcula() {
        Long id = dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0)).dependencia().id();
        var r = dependencias.actualizar(id, new DependenciaEdicion(TipoDependencia.SS, 1));
        assertThat(r.dependencia().tipo()).isEqualTo(TipoDependencia.SS);
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(3)); // 1 dia habil despues del inicio de A
        assertThat(r.cambios()).anyMatch(x -> x.codigo().equals("1.2"));
    }

    @Test
    void eliminarLaDependenciaDevuelveLaTareaAlInicio() {
        Long id = dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0)).dependencia().id();
        var r = dependencias.eliminar(id);
        assertThat(tareas.obtener(b.id).inicioPlan).isEqualTo(d(2));
        assertThat(r.cambios()).anyMatch(x -> x.codigo().equals("1.2") && d(2).equals(x.inicioDespues()));
        assertThat(dependencias.listar(proyectoId)).isEmpty();
    }

    @Test
    void unProyectoTerminadoNoAceptaDependencias() {
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }

    @Test
    void unaTareaConDependenciasNoRecibeSubtareas() {
        dependencias.crear(proyectoId, dep(a, b, TipoDependencia.FS, 0));
        assertThatThrownBy(() -> tareas.crear(proyectoId, new TareaRequest(a.id, "Sub", null, 1, null, null)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TAREA_CON_DEPENDENCIAS");
    }
}
