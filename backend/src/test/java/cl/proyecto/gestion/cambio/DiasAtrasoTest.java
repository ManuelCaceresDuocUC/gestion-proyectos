package cl.proyecto.gestion.cambio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** C-03: días de atraso se calculan, no se persisten. */
class DiasAtrasoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    @Test
    void sinFinPrevistoEsCero() {
        assertThat(SolicitudCambioResponse.diasAtraso(solicitud(null, null, EstadoSolicitud.ABIERTO), HOY))
            .isZero();
    }

    @Test
    void conFinRealUsaEsaFecha() {
        SolicitudCambio s = solicitud(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), EstadoSolicitud.CERRADO);
        assertThat(SolicitudCambioResponse.diasAtraso(s, HOY)).isEqualTo(9);
    }

    @Test
    void finRealAntesDelPrevistoEsCero() {
        SolicitudCambio s = solicitud(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), EstadoSolicitud.CERRADO);
        assertThat(SolicitudCambioResponse.diasAtraso(s, HOY)).isZero();
    }

    @Test
    void abiertaAtrasadaUsaHoy() {
        SolicitudCambio s = solicitud(LocalDate.of(2026, 11, 1), null, EstadoSolicitud.ABIERTO);
        assertThat(SolicitudCambioResponse.diasAtraso(s, HOY)).isEqualTo(11);
    }

    @Test
    void cerradaORechazadaSinFinRealEsCero() {
        LocalDate fin = LocalDate.of(2026, 11, 1);
        assertThat(SolicitudCambioResponse.diasAtraso(solicitud(fin, null, EstadoSolicitud.CERRADO), HOY)).isZero();
        assertThat(SolicitudCambioResponse.diasAtraso(solicitud(fin, null, EstadoSolicitud.RECHAZADO), HOY)).isZero();
    }

    @Test
    void enEvaluacionTambienCuentaAtraso() {
        SolicitudCambio s = solicitud(LocalDate.of(2026, 11, 1), null, EstadoSolicitud.EN_EVALUACION);
        assertThat(SolicitudCambioResponse.diasAtraso(s, HOY)).isEqualTo(11);
    }

    private static SolicitudCambio solicitud(LocalDate finPrevisto, LocalDate finReal, EstadoSolicitud estado) {
        SolicitudCambio s = new SolicitudCambio();
        s.finPrevisto = finPrevisto;
        s.finReal = finReal;
        s.estado = estado;
        return s;
    }
}
