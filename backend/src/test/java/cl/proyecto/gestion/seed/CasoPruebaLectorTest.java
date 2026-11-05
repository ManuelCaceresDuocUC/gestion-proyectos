package cl.proyecto.gestion.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** C-07: el JSON del caso mínimo se lee y se puede mapear a proyecto, recursos y EDT. */
class CasoPruebaLectorTest {

    @Test
    void leeProyectoTiposProfesionalesRecursosYEdt() {
        CasoPrueba c = CasoPruebaLector.leer();

        assertThat(c.proyecto().codigo()).isEqualTo("EEC-2026");
        assertThat(c.proyecto().nombre()).contains("estudio colaborativo");
        assertThat(c.proyecto().fechaInicio()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(c.proyecto().diasLaborales()).isEqualTo("LMXJV");
        assertThat(c.proyecto().responsable()).isEqualTo("P1");

        assertThat(c.tiposRecurso()).extracting(CasoTipoRecurso::nombre)
            .containsExactly("Profesional", "Equipamiento", "Material", "Servicio externo");
        assertThat(c.profesionales()).hasSize(5);
        assertThat(c.profesionales()).extracting(CasoProfesional::clave)
            .containsExactly("P1", "P2", "P3", "P4", "P5");
        assertThat(c.recursos()).hasSize(7);
        assertThat(c.recursos()).extracting(CasoRecurso::clave)
            .contains("E1", "M1", "S1");

        assertThat(c.estructura()).hasSize(4);
        assertThat(c.actividades()).hasSize(12);
        assertThat(c.subactividades()).hasSize(20);
        assertThat(c.actividades()).filteredOn(CasoActividad::esHoja)
            .extracting(CasoActividad::codigo).contains("1.4");
    }

    @Test
    void unaSemanaSonCincoDiasHabiles() {
        assertThat(CasoPruebaLector.diasDe(2)).isEqualTo(10);
        assertThat(CasoPruebaLector.diasDe(1)).isEqualTo(5);
        assertThat(CasoPruebaLector.diasDe(null)).isZero();
    }
}
