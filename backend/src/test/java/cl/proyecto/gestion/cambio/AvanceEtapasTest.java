package cl.proyecto.gestion.cambio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** C-04: avance = completas / 10. No se persiste. */
class AvanceEtapasTest {

    @Test
    void todasPendientesEsCero() {
        List<CambioEtapa> lista = diez(EstadoEtapa.PENDIENTE);
        assertThat(CambioEtapaService.etapasCompletas(lista)).isZero();
        assertThat(CambioEtapaService.avancePorcentaje(lista)).isZero();
        assertThat(CambioEtapaService.etapasBloqueadas(lista)).isZero();
    }

    @Test
    void tresCompletasEs30() {
        List<CambioEtapa> lista = diez(EstadoEtapa.PENDIENTE);
        lista.get(0).estado = EstadoEtapa.COMPLETADO;
        lista.get(1).estado = EstadoEtapa.COMPLETADO;
        lista.get(2).estado = EstadoEtapa.COMPLETADO;
        assertThat(CambioEtapaService.etapasCompletas(lista)).isEqualTo(3);
        assertThat(CambioEtapaService.avancePorcentaje(lista)).isEqualTo(30);
    }

    @Test
    void noAplicaNoSumaAlAvance() {
        List<CambioEtapa> lista = diez(EstadoEtapa.NO_APLICA);
        lista.get(0).estado = EstadoEtapa.COMPLETADO;
        assertThat(CambioEtapaService.etapasCompletas(lista)).isEqualTo(1);
        assertThat(CambioEtapaService.avancePorcentaje(lista)).isEqualTo(10);
    }

    @Test
    void bloqueadasSeCuentanAparte() {
        List<CambioEtapa> lista = diez(EstadoEtapa.PENDIENTE);
        lista.get(3).estado = EstadoEtapa.BLOQUEADO;
        lista.get(4).estado = EstadoEtapa.BLOQUEADO;
        lista.get(0).estado = EstadoEtapa.COMPLETADO;
        assertThat(CambioEtapaService.etapasBloqueadas(lista)).isEqualTo(2);
        assertThat(CambioEtapaService.avancePorcentaje(lista)).isEqualTo(10);
    }

    private static List<CambioEtapa> diez(EstadoEtapa estado) {
        List<CambioEtapa> lista = new ArrayList<>();
        for (EtapasCambio def : EtapasCambio.values()) {
            CambioEtapa e = new CambioEtapa();
            e.numero = (short) def.numero;
            e.nombre = def.nombre;
            e.estado = estado;
            lista.add(e);
        }
        return lista;
    }
}
