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

/** C-03 / C-08. Usa la base real; cada test se revierte al terminar. */
@SpringBootTest
@Transactional
class SolicitudCambioServiceTest {

    @Autowired SolicitudCambioService servicio;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired CambioHistorialRepository historial;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
            "T-C03-" + System.nanoTime() % 1_000_000,
            "Cambio",
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
        SolicitudCambioRequest r = req("X", TipoSolicitud.ALCANCE, EstadoSolicitud.CERRADO);
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
        servicio.actualizar(costo.id, req("Costo", TipoSolicitud.COSTO, EstadoSolicitud.EN_EVALUACION));

        List<SolicitudCambio> abiertas = servicio.listar(
            proyectoId, EstadoSolicitud.ABIERTO, null, null, null, null, null);
        assertThat(abiertas).extracting(s -> s.tipo).containsExactly(TipoSolicitud.TIEMPO);

        List<SolicitudCambio> costos = servicio.listar(
            proyectoId, null, TipoSolicitud.COSTO, null, null, null, null);
        assertThat(costos).hasSize(1);
        assertThat(costos.get(0).estado).isEqualTo(EstadoSolicitud.EN_EVALUACION);
    }

    @Test
    void filtraPorRangoDeFechaSolicitud() {
        servicio.crear(proyectoId, conFecha("Temprana", LocalDate.of(2026, 10, 1)));
        servicio.crear(proyectoId, conFecha("Tardia", LocalDate.of(2026, 10, 20)));

        List<SolicitudCambio> desde = servicio.listar(
            proyectoId, null, null, null, null, LocalDate.of(2026, 10, 10), null);
        assertThat(desde).extracting(s -> s.nombre).containsExactly("Tardia");

        List<SolicitudCambio> hasta = servicio.listar(
            proyectoId, null, null, null, null, null, LocalDate.of(2026, 10, 10));
        assertThat(hasta).extracting(s -> s.nombre).containsExactly("Temprana");
    }

    @Test
    void tareaDeOtroProyectoEsInvalida() {
        Proyecto otro = proyectos.crear(new ProyectoRequest(
            "T-C03B-" + System.nanoTime() % 1_000_000,
            "Otro",
            null, null, null, BigDecimal.ZERO, null, null, null));
        Tarea tarea = tareas.crear(otro.id, new TareaRequest(null, "Etapa", null, null, null, null));
        SolicitudCambioRequest r = req("Mal", TipoSolicitud.ALCANCE, null, null, null, null, null, null, tarea.id);
        assertThatThrownBy(() -> servicio.crear(proyectoId, r))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TAREA_DE_OTRO_PROYECTO");
    }

    @Test
    void putNoPermiteSaltarAAprobado() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Salto", TipoSolicitud.TIEMPO));
        assertThatThrownBy(() -> servicio.actualizar(
                s.id, req("Salto", TipoSolicitud.TIEMPO, EstadoSolicitud.APROBADO)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TRANSICION_INVALIDA");
        assertThat(servicio.obtener(s.id).estado).isEqualTo(EstadoSolicitud.ABIERTO);
    }

    @Test
    void aprobarSoloDesdeEvaluacionYNoAplica() {
        SolicitudCambio s = enEvaluacion("Aprobar");
        SolicitudCambio aprobada = servicio.aprobar(s.id);
        assertThat(aprobada.estado).isEqualTo(EstadoSolicitud.APROBADO);
        assertThat(aprobada.aprobadaEn).isNotNull();
        assertThat(aprobada.aplicada).isFalse();
        assertThat(aprobada.aplicadaEn).isNull();
    }

    @Test
    void aprobarAbiertaEsIlegal() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Temprano", TipoSolicitud.TIEMPO));
        assertThatThrownBy(() -> servicio.aprobar(s.id))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TRANSICION_INVALIDA");
        assertThat(servicio.obtener(s.id).estado).isEqualTo(EstadoSolicitud.ABIERTO);
        assertThat(servicio.obtener(s.id).aprobadaEn).isNull();
    }

    @Test
    void rechazarDesdeEvaluacion() {
        SolicitudCambio s = enEvaluacion("Rechazar");
        SolicitudCambio rechazada = servicio.rechazar(s.id);
        assertThat(rechazada.estado).isEqualTo(EstadoSolicitud.RECHAZADO);
        assertThat(rechazada.aprobadaEn).isNull();
        assertThat(rechazada.aplicada).isFalse();
    }

    @Test
    void evaluacionPuedeVolverAAbierto() {
        SolicitudCambio s = enEvaluacion("Vuelve");
        SolicitudCambio abierta = servicio.actualizar(s.id, req("Vuelve", TipoSolicitud.TIEMPO, EstadoSolicitud.ABIERTO));
        assertThat(abierta.estado).isEqualTo(EstadoSolicitud.ABIERTO);
    }

    @Test
    void flujoAprobadoHastaCerrado() {
        SolicitudCambio s = enEvaluacion("Cadena");
        servicio.aprobar(s.id);
        servicio.actualizar(s.id, req("Cadena", TipoSolicitud.TIEMPO, EstadoSolicitud.EN_IMPLEMENTACION));
        SolicitudCambio cerrada = servicio.actualizar(s.id, req("Cadena", TipoSolicitud.TIEMPO, EstadoSolicitud.CERRADO));
        assertThat(cerrada.estado).isEqualTo(EstadoSolicitud.CERRADO);
        assertThat(cerrada.aprobadaEn).isNotNull();
        assertThatThrownBy(() -> servicio.actualizar(s.id, req("Cadena", TipoSolicitud.TIEMPO, EstadoSolicitud.ABIERTO)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TRANSICION_INVALIDA");
    }

    @Test
    void rechazadaEsTerminal() {
        SolicitudCambio s = enEvaluacion("Fin");
        servicio.rechazar(s.id);
        assertThatThrownBy(() -> servicio.actualizar(s.id, req("Fin", TipoSolicitud.TIEMPO, EstadoSolicitud.ABIERTO)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TRANSICION_INVALIDA");
        assertThatThrownBy(() -> servicio.aprobar(s.id))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("TRANSICION_INVALIDA");
    }

    @Test
    void noAprobarNiRechazarSiElProyectoEstaCerrado() {
        SolicitudCambio s = enEvaluacion("Tarde");
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> servicio.aprobar(s.id))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
        assertThatThrownBy(() -> servicio.rechazar(s.id))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
        assertThat(servicio.obtener(s.id).estado).isEqualTo(EstadoSolicitud.EN_EVALUACION);
    }

    @Test
    void cambiarEstadoEscribeHistorial() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Bitacora", TipoSolicitud.TIEMPO));
        servicio.actualizar(s.id, req("Bitacora", TipoSolicitud.TIEMPO, EstadoSolicitud.EN_EVALUACION));
        servicio.aprobar(s.id);

        List<CambioHistorial> bitacora = historial.findBySolicitudIdOrderByFechaAsc(s.id);
        assertThat(bitacora).extracting(h -> h.campo).containsExactly("estado", "estado");
        assertThat(bitacora.get(0).valorAnterior).isEqualTo("ABIERTO");
        assertThat(bitacora.get(0).valorNuevo).isEqualTo("EN_EVALUACION");
        assertThat(bitacora.get(1).valorAnterior).isEqualTo("EN_EVALUACION");
        assertThat(bitacora.get(1).valorNuevo).isEqualTo("APROBADO");
    }

    @Test
    void mismoEstadoNoEscribeHistorial() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Igual", TipoSolicitud.TIEMPO));
        servicio.actualizar(s.id, req("Igual", TipoSolicitud.TIEMPO, EstadoSolicitud.ABIERTO));
        assertThat(historial.findBySolicitudIdOrderByFechaAsc(s.id)).isEmpty();
    }

    @Test
    void saltoIlegalNoEscribeHistorial() {
        SolicitudCambio s = servicio.crear(proyectoId, req("Salto", TipoSolicitud.TIEMPO));
        assertThatThrownBy(() -> servicio.aprobar(s.id)).isInstanceOf(ApiException.class);
        assertThat(historial.findBySolicitudIdOrderByFechaAsc(s.id)).isEmpty();
    }

    @Test
    void actualizarNoCambiaCodigoNiAplicada() {
        SolicitudCambio creada = servicio.crear(proyectoId, req("Original", TipoSolicitud.TIEMPO));
        SolicitudCambioRequest r = req(
            "Nuevo nombre",
            TipoSolicitud.TIEMPO,
            EstadoSolicitud.EN_EVALUACION,
            "justificacion",
            null,
            LocalDate.of(2026, 12, 1),
            PrioridadSolicitud.ALTA,
            3,
            null);
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
        SolicitudCambioRequest r = req(
            "Atraso", TipoSolicitud.TIEMPO, null, null, null, LocalDate.of(2026, 11, 1), null, null, null);
        SolicitudCambio s = servicio.crear(proyectoId, r);
        SolicitudCambioResponse json = SolicitudCambioResponse.de(s, LocalDate.of(2026, 11, 12));
        assertThat(json.diasAtraso()).isEqualTo(11);
    }

    private SolicitudCambio enEvaluacion(String nombre) {
        SolicitudCambio s = servicio.crear(proyectoId, req(nombre, TipoSolicitud.TIEMPO));
        return servicio.actualizar(s.id, req(nombre, TipoSolicitud.TIEMPO, EstadoSolicitud.EN_EVALUACION));
    }

    private static SolicitudCambioRequest req(String nombre, TipoSolicitud tipo) {
        return req(nombre, tipo, null, null, null, null, null, null, null);
    }

    private static SolicitudCambioRequest req(String nombre, TipoSolicitud tipo, EstadoSolicitud estado) {
        return req(nombre, tipo, estado, null, null, null, null, null, null);
    }

    private static SolicitudCambioRequest conFecha(String nombre, LocalDate fecha) {
        return req(nombre, TipoSolicitud.ALCANCE, null, null, fecha, null, null, null, null);
    }

    /** Un solo constructor de 23 campos, para no contar nulls a mano en cada test. */
    private static SolicitudCambioRequest req(
            String nombre,
            TipoSolicitud tipo,
            EstadoSolicitud estado,
            String descripcion,
            LocalDate fechaSolicitud,
            LocalDate finPrevisto,
            PrioridadSolicitud prioridad,
            Integer impactoDias,
            Long tareaId) {
        return new SolicitudCambioRequest(
            nombre,
            descripcion,
            null,
            null,
            fechaSolicitud,
            null,
            finPrevisto,
            null,
            prioridad,
            tipo,
            null,
            null,
            estado,
            tareaId,
            impactoDias,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    }
}
