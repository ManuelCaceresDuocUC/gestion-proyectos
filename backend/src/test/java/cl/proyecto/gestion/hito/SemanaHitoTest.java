package cl.proyecto.gestion.hito;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Semana 1 del proyecto que empieza el lunes 2026-09-07 termina el viernes 2026-09-11. */
class SemanaHitoTest {

    private final Calendario calendario = new Calendario(Set.of());
    private final LocalDate inicio = LocalDate.of(2026, 9, 7);

    @Test
    void laFechaPrevistaEsElViernesDeEsaSemana() {
        assertThat(SemanaHito.fechaDeSemana(inicio, calendario, 1)).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(SemanaHito.fechaDeSemana(inicio, calendario, 3)).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(SemanaHito.fechaDeSemana(inicio, calendario, 12)).isEqualTo(LocalDate.of(2026, 11, 27));
    }

    @Test
    void laSemanaSeRecuperaDesdeLaFecha() {
        for (int semana = 1; semana <= 12; semana++) {
            LocalDate fecha = SemanaHito.fechaDeSemana(inicio, calendario, semana);
            assertThat(SemanaHito.semanaDe(inicio, calendario, fecha)).isEqualTo(semana);
        }
    }
}
