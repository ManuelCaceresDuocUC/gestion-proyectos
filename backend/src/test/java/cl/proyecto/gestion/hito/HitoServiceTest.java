package cl.proyecto.gestion.hito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.seed.SeedService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** B-11: la fecha no adelanta el término de las actividades y CUMPLIDO exige evidencia. */
@SpringBootTest
@Transactional
class HitoServiceTest {

    @Autowired HitoService hitos;
    @Autowired SeedService seed;
    @Autowired TareaRepository tareas;

    private HitoResponse h1;

    @BeforeEach
    void cargar() {
        seed.cargar();
        Long proyectoId = tareas.findAll().stream()
            .filter(t -> "1.2.1".equals(t.codigo))
            .map(t -> t.proyectoId)
            .findFirst()
            .orElseThrow();
        h1 = hitos.listar(proyectoId).stream()
            .filter(h -> "H1".equals(h.codigo()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void rechazaLaSemanaAnteriorAlTerminoYLoInforma() {
        assertThatThrownBy(() -> hitos.actualizar(h1.id(), copia(h1, EstadoHito.PENDIENTE, 1, null, h1.evidencia())))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("anterior al término")
            .extracting(e -> ((ApiException) e).getCodigo())
            .isEqualTo("HITO_ANTES_DE_ACTIVIDADES");
        assertThat(hitos.obtener(h1.id()).semanaPrevista()).isEqualTo(3);
    }

    @Test
    void noMarcaCumplidoSinEvidenciaNiFechaRealNiActividadesTerminadas() {
        assertThatThrownBy(() -> hitos.actualizar(h1.id(), copia(h1, EstadoHito.CUMPLIDO, 3, null, "   ")))
            .extracting(e -> ((ApiException) e).getCodigo())
            .isEqualTo("CUMPLIDO_SIN_EVIDENCIA");

        assertThatThrownBy(() -> hitos.actualizar(h1.id(),
                copia(h1, EstadoHito.CUMPLIDO, 3, null, "Acta firmada")))
            .extracting(e -> ((ApiException) e).getCodigo())
            .isEqualTo("CUMPLIDO_SIN_FECHA_REAL");

        assertThatThrownBy(() -> hitos.actualizar(h1.id(),
                copia(h1, EstadoHito.CUMPLIDO, 3, h1.fechaPlan(), "Acta firmada")))
            .extracting(e -> ((ApiException) e).getCodigo())
            .isEqualTo("HITO_ACTIVIDADES_PENDIENTES");

        assertThat(hitos.obtener(h1.id()).estado()).isEqualTo(EstadoHito.PENDIENTE);
    }

    @Test
    void marcaCumplidoCuandoHayEvidenciaFechaRealYLaActividadYaTermino() {
        Tarea actividad = tareas.findById(h1.tareaIds().getFirst()).orElseThrow();
        actividad.finReal = actividad.finPlan;
        tareas.saveAndFlush(actividad);

        HitoResponse cumplido = hitos.actualizar(h1.id(),
            copia(h1, EstadoHito.CUMPLIDO, 3, actividad.finReal, "Acta firmada"));
        assertThat(cumplido.estado()).isEqualTo(EstadoHito.CUMPLIDO);
        assertThat(cumplido.fechaReal()).isEqualTo(actividad.finReal);

        LocalDate antes = actividad.finReal.minusDays(3);
        assertThatThrownBy(() -> hitos.actualizar(h1.id(),
                copia(h1, EstadoHito.CUMPLIDO, 3, antes, "Acta firmada")))
            .extracting(e -> ((ApiException) e).getCodigo())
            .isEqualTo("HITO_ANTES_DE_ACTIVIDADES");
    }

    private static HitoRequest copia(HitoResponse h, EstadoHito estado, int semana, LocalDate fechaReal, String evidencia) {
        return new HitoRequest(
            h.codigo(), h.logro(), semana, evidencia, h.indicador(), h.criterioAceptacion(),
            h.validador(), h.accionSiNoCumple(), estado, fechaReal, h.responsableId(), h.tareaIds());
    }
}
