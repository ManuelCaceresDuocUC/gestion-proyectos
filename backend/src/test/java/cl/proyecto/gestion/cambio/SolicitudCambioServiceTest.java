package cl.proyecto.gestion.cambio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** C-03. Usa la base real; cada test se revierte al terminar. */
@SpringBootTest
@Transactional
class SolicitudCambioServiceTest {

    @Autowired SolicitudCambioService servicio;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-C03-" + System.nanoTime() % 1_000_000, "Cambio",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @Test
    void creaConCodigoEstadoYNoAplicada() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Ampliar plazo", TipoSolicitud.TIEMPO));
        assertThat(s.id).isNotNull();
        assertThat(s.codigo).isEqualTo("SC-01");
        assertThat(s.estado).isEqualTo(EstadoSolicitud.ABIERTO);
        assertThat(s.aplicada).isFalse();
        assertThat(s.prioridad).isEqualTo(PrioridadSolicitud.MEDIA);
        assertThat(s.tipo).isEqualTo(TipoSolicitud.TIEMPO);
    }

    @Test
    void alCrearIgnoraElEstadoMandado() {
        SolicitudCambioRequest r = new SolicitudCambioRequest("X", null, null, null, null, null, null, null,
            null, TipoSolicitud.ALCANCE, null, null, EstadoSolicitud.CERRADO, null, null, null, null, null,
            null, null, null, null, null);
        assertThat(servicio.crear(proyectoId, r).estado).isEqualTo(EstadoSolicitud.ABIERTO);
    }

    @Test
    void segundoCodigoEsSC02() {
        servicio.crear(proyectoId, req("Uno", TipoSolicitud.COSTO));
        assertThat(servicio.crear(proyectoId, req("Dos", TipoSolicitud.COSTO)).codigo).isEqualTo("SC-02");
    }

    @Test
    void noCreaEnProyectoCerrado() {
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> servicio.crear(proyectoId, req("Tarde", TipoSolicitud.ALCANCE)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }

    @Test
    void obtenerInexistenteEs404() {
        assertThatThrownBy(() -> servicio.obtener(-1L))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("NO_ENCONTRADO");
    }

    @Test
    void filtraPorEstadoYTipo() {
        servicio.crear(proyectoId, req("Tiempo", TipoSolicitud.TIEMPO));
        SolicitudCambio costo = servicio.crear(proyectoId, req("Costo", TipoSolicitud.COSTO));
        servicio.actualizar(costo.id, conEstado(req("Costo", TipoSolicitud.COSTO), EstadoSolicitud.EN_EVALUACION));

        List<SolicitudCambio> abiertas = servicio.listar(proyectoId, EstadoSolicitud.ABIERTO, null, null, null, null, null);
        assertThat(abiertas).extracting(s -> s.tipo).containsExactly(TipoSolicitud.TIEMPO);

        List<SolicitudCambio> costos = servicio.listar(proyectoId, null, TipoSolicitud.COSTO, null, null, null, null);
        assertThat(costos).hasSize(1);
        assertThat(costos.get(0).estado).isEqualTo(EstadoSolicitud.EN_EVALUACION);
    }

    @Test
    void filtraPorRangoDeFechaSolicitud() {
        servicio.crear(proyectoId, conFecha("Temprana", LocalDate.of(2026, 10, 1)));
        servicio.crear(proyectoId, conFecha("Tardia", LocalDate.of(2026, 10, 20)));

        List<SolicitudCambio> desde = servicio.listar(proyectoId, null, null, null, null,
            LocalDate.of(2026, 10, 10), null);
        assertThat(desde).extracting(s -> s.nombre).containsExactly("Tardia");

        List<SolicitudCambio> hasta = servicio.listar(proyectoId, null, null, null, null,
            null, LocalDate.of(2026, 10, 10));
        assertThat(hasta).extracting(s -> s.nombre).containsExactly("Temprana");
    }

    @Test
    void tareaDeOtroProyectoEsInvalida() {
        Proyecto otro = proyectos.crear(new ProyectoRequest("T-C03B-" + System.nanoTime() % 1_000_000, "Otro",
            null, null, null, BigDecimal.ZERO, null, null, null));
        Tarea tarea = tareas.crear(otro.id, new TareaRequest(null, "Etapa", null, null, null, null));
        SolicitudCambioRequest r = new SolicitudCambioRequest("Mal", null, null, null, null, null, null, null,
            null, TipoSolicitud.ALCANCE, null, null, null, tarea.id, null, null, null, null, null, null, null,
            null, null);
        assertThatThrownBy(() -> servicio.crear(proyectoId, r))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TAREA_DE_OTRO_PROYECTO");
    }

    @Test
    void actualizarNoCambiaCodigoNiAplicada() {
        SolicitudCambio creada = servicio.crear(proyectoId, req("Original", TipoSolicitud.TIEMPO));
        SolicitudCambioRequest r = new SolicitudCambioRequest("Nuevo nombre", "justificacion", null, null, null,
            null, LocalDate.of(2026, 12, 1), null, PrioridadSolicitud.ALTA, TipoSolicitud.TIEMPO, null, null,
            EstadoSolicitud.EN_EVALUACION, null, 3, null, null, null, null, null, null, null, null);
        SolicitudCambio editada = servicio.actualizar(creada.id, r);
        assertThat(editada.codigo).isEqualTo("SC-01");
        assertThat(editada.aplicada).isFalse();
        assertThat(editada.nombre).isEqualTo("Nuevo nombre");
        assertThat(editada.estado).isEqualTo(EstadoSolicitud.EN_EVALUACION);
        assertThat(editada.impactoDias).isEqualTo(3);
    }

    @Test
    void eliminarQuitaLaFila() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Borrar", TipoSolicitud.RECURSO));
        servicio.eliminar(s.id);
        assertThatThrownBy(() -> servicio.obtener(s.id)).isInstanceOf(ApiException.class);
    }

    @Test
    void respuestaIncluyeDiasAtrasoCalculados() {
        SolicitudCambioRequest r = new SolicitudCambioRequest("Atraso", null, null, null, null, null,
            LocalDate.of(2026, 11, 1), null, null, TipoSolicitud.TIEMPO, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        SolicitudCambio s = servicio.crear(proyectoId, r);
        SolicitudCambioResponse json = SolicitudCambioResponse.de(s, LocalDate.of(2026, 11, 12));
        assertThat(json.diasAtraso()).isEqualTo(11);
    }

    private static SolicitudCambioRequest req(String nombre, TipoSolicitud tipo) {
        return new SolicitudCambioRequest(nombre, null, null, null, null, null, null, null,
            null, tipo, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static SolicitudCambioRequest conEstado(SolicitudCambioRequest base, EstadoSolicitud estado) {
        return new SolicitudCambioRequest(base.nombre(), base.descripcion(), base.solicitante(), base.responsableId(),
            base.fechaSolicitud(), base.inicioPrevisto(), base.finPrevisto(), base.finReal(), base.prioridad(),
            base.tipo(), base.riesgo(), base.impacto(), estado, base.tareaId(), base.impactoDias(),
            base.impactoCosto(), base.impactoAlcance(), base.impactoRecursos(), base.resultadoEvaluacion(),
            base.presupuestoPrevisto(), base.costoReal(), base.accionCorrectiva(), base.resultado());
    }

    private static SolicitudCambioRequest conFecha(String nombre, LocalDate fecha) {
        return new SolicitudCambioRequest(nombre, null, null, null, fecha, null, null, null,
            null, TipoSolicitud.ALCANCE, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }
}
