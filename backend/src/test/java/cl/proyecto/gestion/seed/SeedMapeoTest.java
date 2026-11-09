package cl.proyecto.gestion.seed;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.cambio.ImpactoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.TipoSolicitud;
import org.junit.jupiter.api.Test;

/** C-11: textos del JSON viejo → enums V21. */
class SeedMapeoTest {

    @Test
    void mapeaTiposDelJson() {
        assertThat(SeedMapeo.tipoDe("Tecnología")).isEqualTo(TipoSolicitud.TECNOLOGIA);
        assertThat(SeedMapeo.tipoDe("Personas")).isEqualTo(TipoSolicitud.ORGANIZACION);
        assertThat(SeedMapeo.tipoDe("Proceso")).isEqualTo(TipoSolicitud.ORGANIZACION);
        assertThat(SeedMapeo.tipoDe("ALCANCE")).isEqualTo(TipoSolicitud.ALCANCE);
    }

    @Test
    void mapeaPrioridadRiesgoEImpacto() {
        assertThat(SeedMapeo.prioridadDe("Crítica")).isEqualTo(PrioridadSolicitud.CRITICA);
        assertThat(SeedMapeo.prioridadDe("Baja")).isEqualTo(PrioridadSolicitud.BAJA);
        assertThat(SeedMapeo.riesgoDe("Medio")).isEqualTo(RiesgoSolicitud.MEDIO);
        assertThat(SeedMapeo.impactoDe("Alto")).isEqualTo(ImpactoSolicitud.ALTO);
        assertThat(SeedMapeo.prioridadDe(null)).isNull();
        assertThat(SeedMapeo.prioridadDe("  ")).isNull();
    }
}
