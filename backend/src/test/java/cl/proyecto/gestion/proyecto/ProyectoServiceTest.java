package cl.proyecto.gestion.proyecto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** A-04. Usa la base real (docker compose up db); cada test se revierte al terminar. */
@SpringBootTest
@Transactional
class ProyectoServiceTest {

    @Autowired ProyectoService servicio;

    static ProyectoRequest req(String codigo) {
        return new ProyectoRequest(codigo, "Proyecto " + codigo, null, null, null,
            new BigDecimal("1000000"), null, null, null);
    }

    @Test
    void creaConValoresPorDefecto() {
        Proyecto p = servicio.crear(req("T-A04-1"));
        assertThat(p.id).isNotNull();
        assertThat(p.estado).isEqualTo(EstadoProyecto.PLANIFICADO);
        assertThat(p.moneda).isEqualTo("CLP");
        assertThat(p.diasLaborales).isEqualTo("LMXJV");
    }

    @Test
    void codigoDuplicadoEsConflicto() {
        servicio.crear(req("T-A04-2"));
        assertThatThrownBy(() -> servicio.crear(req("T-A04-2")))
            .isInstanceOf(ApiException.class)
            .extracting("codigo").isEqualTo("CODIGO_DUPLICADO");
    }

    @Test
    void noSeCreaCerrado() {
        ProyectoRequest r = new ProyectoRequest("T-A04-3", "X", null, EstadoProyecto.CERRADO, null,
            BigDecimal.ONE, null, null, null);
        assertThatThrownBy(() -> servicio.crear(r))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("USAR_CERRAR");
    }

    @Test
    void cerrarBloqueaEdicion() {
        Proyecto p = servicio.crear(req("T-A04-4"));
        Proyecto c = servicio.cerrar(p.id);
        assertThat(c.estado).isEqualTo(EstadoProyecto.CERRADO);
        assertThat(c.fechaFinReal).isNotNull();
        assertThatThrownBy(() -> servicio.actualizar(p.id, req("T-A04-4")))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }

    @Test
    void obtenerInexistenteEs404() {
        assertThatThrownBy(() -> servicio.obtener(-1L)).isInstanceOf(ApiException.class);
    }
}
