package cl.proyecto.gestion.planificacion.calendario;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CalendarioProyectosTest {

    @Autowired ProyectoService proyectos;
    @Autowired NoLaborableService noLaborables;
    @Autowired CalendarioProyectos calendarios;

    @Test
    void excluyeFeriadosGeneralesYDelProyecto() {
        var proyecto = proyectos.crear(new ProyectoRequest(
            "T-B07-CAL-" + System.nanoTime() % 1_000_000,
            "Calendario",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            "LMXJV",
            LocalDate.of(2026, 11, 2)));
        noLaborables.crear(new NoLaborableRequest(null, LocalDate.of(2026, 11, 3), "General"));
        noLaborables.crear(new NoLaborableRequest(proyecto.id, LocalDate.of(2026, 11, 4), "Proyecto"));

        var calendario = calendarios.de(proyecto);

        assertThat(calendario.esLaborable(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(calendario.esLaborable(LocalDate.of(2026, 11, 3))).isFalse();
        assertThat(calendario.esLaborable(LocalDate.of(2026, 11, 4))).isFalse();
        assertThat(calendario.diasHabilesEntre(
            LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6))).isEqualTo(3);
    }
}
