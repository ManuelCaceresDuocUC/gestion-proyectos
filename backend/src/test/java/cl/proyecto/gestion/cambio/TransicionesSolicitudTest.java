package cl.proyecto.gestion.cambio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import org.junit.jupiter.api.Test;

/** C-08: grafo de transiciones, sin Spring ni base de datos. */
class TransicionesSolicitudTest {

    @Test
    void pasosValidos() {
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.ABIERTO, EstadoSolicitud.EN_EVALUACION)).isTrue();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.EN_EVALUACION, EstadoSolicitud.ABIERTO)).isTrue();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.EN_EVALUACION, EstadoSolicitud.APROBADO)).isTrue();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.EN_EVALUACION, EstadoSolicitud.RECHAZADO)).isTrue();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.APROBADO, EstadoSolicitud.EN_IMPLEMENTACION)).isTrue();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.EN_IMPLEMENTACION, EstadoSolicitud.CERRADO)).isTrue();
    }

    @Test
    void elMismoEstadoEsValido() {
        for (EstadoSolicitud e : EstadoSolicitud.values()) {
            assertThat(TransicionesSolicitud.permite(e, e)).isTrue();
        }
    }

    @Test
    void noSaltaEtapasNiSaleDeUnTerminal() {
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.ABIERTO, EstadoSolicitud.APROBADO)).isFalse();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.APROBADO, EstadoSolicitud.CERRADO)).isFalse();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.RECHAZADO, EstadoSolicitud.ABIERTO)).isFalse();
        assertThat(TransicionesSolicitud.permite(EstadoSolicitud.CERRADO, EstadoSolicitud.EN_EVALUACION)).isFalse();
    }

    @Test
    void saltoIlegalLanza409() {
        assertThatThrownBy(() -> TransicionesSolicitud.exigir(
                EstadoSolicitud.ABIERTO, EstadoSolicitud.APROBADO))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("ABIERTO")
            .satisfies(e -> {
                ApiException api = (ApiException) e;
                assertThat(api.getCodigo()).isEqualTo("TRANSICION_INVALIDA");
                assertThat(api.getStatus().value()).isEqualTo(409);
            });
    }

    @Test
    void aprobarYRechazarSoloDesdeEvaluacion() {
        assertThat(TransicionesSolicitud.puedeAprobar(EstadoSolicitud.EN_EVALUACION)).isTrue();
        assertThat(TransicionesSolicitud.puedeRechazar(EstadoSolicitud.EN_EVALUACION)).isTrue();
        assertThat(TransicionesSolicitud.puedeAprobar(EstadoSolicitud.ABIERTO)).isFalse();
        assertThat(TransicionesSolicitud.puedeRechazar(EstadoSolicitud.APROBADO)).isFalse();
    }

    @Test
    void rechazadoYCerradoSonTerminales() {
        assertThat(TransicionesSolicitud.esTerminal(EstadoSolicitud.RECHAZADO)).isTrue();
        assertThat(TransicionesSolicitud.esTerminal(EstadoSolicitud.CERRADO)).isTrue();
        assertThat(TransicionesSolicitud.esTerminal(EstadoSolicitud.APROBADO)).isFalse();
    }
}
