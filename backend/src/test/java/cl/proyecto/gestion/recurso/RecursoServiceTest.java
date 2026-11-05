package cl.proyecto.gestion.recurso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecursoServiceTest {

    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;

    @Test
    void creaTipoDeRecurso() {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        assertThat(tipo.id).isNotNull();
        assertThat(tipo.nombre).startsWith("Profesional-");
        assertThat(tipo.unidad).isEqualTo("hora");
    }

    @Test
    void rechazaTipoDuplicado() {
        String nombre = "Profesional-" + System.nanoTime();
        tipos.crear(new TipoRecursoRequest(nombre, "hora"));

        assertThatThrownBy(() -> tipos.crear(new TipoRecursoRequest(nombre, "hora")))
            .isInstanceOf(ApiException.class)
            .extracting("codigo")
            .isEqualTo("TIPO_RECURSO_DUPLICADO");
    }

    @Test
    void creaRecursoConTipoExistente() {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        Recurso recurso = recursos.crear(new RecursoRequest(
            tipo.id,
            "Ana Pérez",
            new BigDecimal("25000.00"),
            new BigDecimal("8.0"),
            true));

        assertThat(recurso.id).isNotNull();
        assertThat(recurso.tipoRecursoId).isEqualTo(tipo.id);
        assertThat(recurso.tarifa).isEqualByComparingTo("25000.00");
        assertThat(recurso.capacidadHorasDia).isEqualByComparingTo("8.0");
        assertThat(recurso.activo).isTrue();
    }

    @Test
    void rechazaRecursoConTipoInexistente() {
        assertThatThrownBy(() -> recursos.crear(new RecursoRequest(
            -1L,
            "Ana Pérez",
            new BigDecimal("25000.00"),
            new BigDecimal("8.0"),
            true)))
            .isInstanceOf(ApiException.class)
            .extracting("codigo")
            .isEqualTo("TIPO_RECURSO_INEXISTENTE");
    }

    @Test
    void listaSoloRecursosActivos() {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        recursos.crear(new RecursoRequest(
            tipo.id, "Activo", BigDecimal.ONE, BigDecimal.ONE, true));

        recursos.crear(new RecursoRequest(
            tipo.id, "Inactivo", BigDecimal.ONE, BigDecimal.ONE, false));

        assertThat(recursos.listar(true))
            .extracting(recurso -> recurso.nombre)
            .contains("Activo")
            .doesNotContain("Inactivo");
    }
}