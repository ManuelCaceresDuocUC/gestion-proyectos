package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.CambioEtapaRepository;
import cl.proyecto.gestion.cambio.EstadoEtapa;
import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.cambio.SolicitudCambioRepository;
import cl.proyecto.gestion.cambio.SolicitudCambioRequest;
import cl.proyecto.gestion.cambio.SolicitudCambioService;
import cl.proyecto.gestion.cambio.TipoSolicitud;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/** Las variables salen de filas guardadas, con la misma cuenta que el dominio puro. */
@SpringBootTest
@Transactional
@WithMockUser(roles = "ADMIN")
class VariablesCambioServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    @Autowired VariablesCambioService variables;
    @Autowired SolicitudCambioService cambios;
    @Autowired SolicitudCambioRepository solicitudes;
    @Autowired CambioEtapaRepository etapas;
    @Autowired ProyectoService proyectos;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
                "T-B17-" + System.nanoTime() % 1_000_000,
                "KPI cambio",
                null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @Test
    void proyectoSinSolicitudesQuedaEnCero() {
        Map<String, BigDecimal> v = variables.deProyecto(proyectoId, HOY);

        assertThat(v.get(VariablesCambio.CAMBIOS_TOTAL)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.ETAPAS_TOTALES)).isEqualByComparingTo("0");
        assertThat(v.values()).allMatch(valor -> valor.compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void coincideConLosRegistrosGuardados() {
        SolicitudCambio cerrada = cambios.crear(proyectoId, alta("Cerrada en plazo"));
        cerrada.estado = EstadoSolicitud.CERRADO;
        cerrada.finPrevisto = LocalDate.of(2026, 11, 1);
        cerrada.finReal = LocalDate.of(2026, 11, 10);
        cerrada.riesgo = RiesgoSolicitud.BAJO;
        cerrada.presupuestoPrevisto = new BigDecimal("80.00");
        cerrada.costoReal = new BigDecimal("80.00");
        cerrada.aplicada = true;
        solicitudes.save(cerrada);
        etapa(cerrada.id, 10, EstadoEtapa.COMPLETADO);

        SolicitudCambio abierta = cambios.crear(proyectoId, alta("Abierta atrasada"));
        abierta.finPrevisto = LocalDate.of(2026, 11, 1);
        abierta.prioridad = PrioridadSolicitud.ALTA;
        solicitudes.save(abierta);
        etapa(abierta.id, 4, EstadoEtapa.BLOQUEADO);

        Map<String, BigDecimal> v = variables.deProyecto(proyectoId, HOY);

        assertThat(v.get(VariablesCambio.CAMBIOS_TOTAL)).isEqualByComparingTo("2");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_EN_PLAZO)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.CAMBIOS_CON_ATRASO)).isEqualByComparingTo("2");
        assertThat(v.get(VariablesCambio.DIAS_ATRASO_CAMBIOS)).isEqualByComparingTo("20");
        assertThat(v.get(VariablesCambio.CAMBIOS_PRIORIDAD_ALTA)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CON_BLOQUEO)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_ADOPTADOS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.ETAPAS_COMPLETADAS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.ETAPAS_TOTALES)).isEqualByComparingTo("20");
        assertThat(v.get(VariablesCambio.CAMBIOS_APROBADOS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_APROBADOS_APLICADOS)).isEqualByComparingTo("1");
        assertThat(v.get(VariablesCambio.CAMBIOS_CERRADOS_CON_SOBRECOSTO)).isEqualByComparingTo("0");
        assertThat(v.get(VariablesCambio.COSTO_REAL_CAMBIOS_CERRADOS)).isEqualByComparingTo("80.00");
        assertThat(v.get(VariablesCambio.PRESUPUESTO_CAMBIOS_CERRADOS)).isEqualByComparingTo("80.00");

        var kc01 = new EvaluadorKpi().evaluar(CatalogoKpiCambio.porCodigo("KC-01").definicion(), v);
        assertThat(kc01.valor()).isEqualByComparingTo("50.00");
        assertThat(kc01.cumpleMeta()).isFalse();
    }

    private void etapa(Long solicitudId, int numero, EstadoEtapa estado) {
        CambioEtapa e = etapas.findBySolicitudIdAndNumero(solicitudId, (short) numero).orElseThrow();
        e.estado = estado;
        etapas.save(e);
    }

    private static SolicitudCambioRequest alta(String nombre) {
        return new SolicitudCambioRequest(
                nombre, null, null, null, null, null, null, null,
                null, TipoSolicitud.ALCANCE, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
