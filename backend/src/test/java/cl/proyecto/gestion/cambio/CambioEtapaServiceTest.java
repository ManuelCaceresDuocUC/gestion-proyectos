package cl.proyecto.gestion.cambio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** C-04. Usa la base real; cada test se revierte al terminar. */
@SpringBootTest
@Transactional
class CambioEtapaServiceTest {

    @Autowired SolicitudCambioService solicitudes;
    @Autowired CambioEtapaService etapas;
    @Autowired CambioHistorialRepository historial;
    @Autowired ProyectoService proyectos;

    Long proyectoId;
    Long solicitudId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-C04-" + System.nanoTime() % 1_000_000, "Etapas",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
        solicitudId = solicitudes.crear(proyectoId, req()).id;
    }

    @Test
    void alCrearGeneraDiezPendientesEnOrden() {
        List<CambioEtapa> lista = etapas.listar(solicitudId);
        assertThat(lista).hasSize(10);
        assertThat(lista).extracting(e -> (int) e.numero).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(lista).extracting(e -> e.nombre).containsExactly(
            "Necesidad", "Evaluación", "Riesgos", "Planificación", "Comunicación inicial",
            "Implementación", "Seguimiento", "Resultados corto plazo", "Correctivas", "Adopción cultural");
        assertThat(lista).allMatch(e -> e.estado == EstadoEtapa.PENDIENTE);
        assertThat(CambioEtapaService.avancePorcentaje(lista)).isZero();
    }

    @Test
    void generarNoDuplica() {
        assertThat(etapas.generar(solicitudId)).hasSize(10);
        assertThat(etapas.listar(solicitudId)).hasSize(10);
    }

    @Test
    void cambiarEstadoEscribeHistorial() {
        CambioEtapa e = etapas.cambiarEstado(solicitudId, (short) 3, EstadoEtapa.EN_CURSO);
        assertThat(e.estado).isEqualTo(EstadoEtapa.EN_CURSO);
        assertThat(e.numero).isEqualTo((short) 3);

        List<CambioHistorial> bitacora = historial.findBySolicitudIdOrderByFechaAsc(solicitudId);
        assertThat(bitacora).hasSize(1);
        assertThat(bitacora.get(0).campo).isEqualTo("etapa.3.estado");
        assertThat(bitacora.get(0).valorAnterior).isEqualTo("PENDIENTE");
        assertThat(bitacora.get(0).valorNuevo).isEqualTo("EN_CURSO");
    }

    @Test
    void mismoEstadoNoEscribeHistorial() {
        etapas.cambiarEstado(solicitudId, (short) 1, EstadoEtapa.PENDIENTE);
        assertThat(historial.findBySolicitudIdOrderByFechaAsc(solicitudId)).isEmpty();
    }

    @Test
    void tresCompletasDanAvance30() {
        etapas.cambiarEstado(solicitudId, (short) 1, EstadoEtapa.COMPLETADO);
        etapas.cambiarEstado(solicitudId, (short) 2, EstadoEtapa.COMPLETADO);
        etapas.cambiarEstado(solicitudId, (short) 3, EstadoEtapa.COMPLETADO);
        List<CambioEtapa> lista = etapas.listar(solicitudId);
        assertThat(CambioEtapaService.etapasCompletas(lista)).isEqualTo(3);
        assertThat(CambioEtapaService.avancePorcentaje(lista)).isEqualTo(30);
    }

    @Test
    void etapaInexistenteEs404() {
        assertThatThrownBy(() -> etapas.cambiarEstado(solicitudId, (short) 11, EstadoEtapa.EN_CURSO))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("NO_ENCONTRADO");
    }

    @Test
    void proyectoCerradoNoCambiaEtapa() {
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> etapas.cambiarEstado(solicitudId, (short) 1, EstadoEtapa.EN_CURSO))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }

    @Test
    void solicitudInexistenteEs404() {
        assertThatThrownBy(() -> etapas.listar(-1L))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("NO_ENCONTRADO");
    }

    private static SolicitudCambioRequest req() {
        return new SolicitudCambioRequest("Cambio C-04", null, null, null, null, null, null, null,
            null, TipoSolicitud.TIEMPO, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }
}
