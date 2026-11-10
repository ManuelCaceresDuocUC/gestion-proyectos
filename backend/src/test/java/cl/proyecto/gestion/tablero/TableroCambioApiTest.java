package cl.proyecto.gestion.tablero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.CambioEtapaRepository;
import cl.proyecto.gestion.cambio.EstadoEtapa;
import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.cambio.SolicitudCambioRepository;
import cl.proyecto.gestion.cambio.SolicitudCambioRequest;
import cl.proyecto.gestion.cambio.SolicitudCambioService;
import cl.proyecto.gestion.cambio.TipoSolicitud;
import cl.proyecto.gestion.kpi.KpiDefinicionRepository;
import cl.proyecto.gestion.kpi.KpiHistoricoRepository;
import cl.proyecto.gestion.kpi.PasoKpi;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class TableroCambioApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired SolicitudCambioService cambios;
    @Autowired SolicitudCambioRepository solicitudes;
    @Autowired CambioEtapaRepository etapas;
    @Autowired KpiHistoricoRepository historicos;
    @Autowired KpiDefinicionRepository definiciones;
    @Autowired PasoKpi pasoKpi;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
                "T-C12-" + System.nanoTime() % 1_000_000,
                "Tablero cambio",
                null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @Test
    void proyectoSinSolicitudesDevuelveCerosYSinKpi() throws Exception {
        int snapshots = historicos.findByProyectoId(proyectoId).size();
        int defs = definiciones.findByProyectoIdIsNullAndActivoTrueOrderByCodigoAsc().size();

        mvc.perform(get("/api/tablero/cambio/{id}", proyectoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proyectoId").value(proyectoId.intValue()))
                .andExpect(jsonPath("$.conteos.total").value(0))
                .andExpect(jsonPath("$.conteos.porEstado.ABIERTO").value(0))
                .andExpect(jsonPath("$.conteos.enCurso").value(0))
                .andExpect(jsonPath("$.etapas.avancePromedio").value(0))
                .andExpect(jsonPath("$.kpi.length()").value(0));

        assertThat(historicos.findByProyectoId(proyectoId)).hasSize(snapshots);
        assertThat(definiciones.findByProyectoIdIsNullAndActivoTrueOrderByCodigoAsc()).hasSize(defs);
    }

    @Test
    void agregaSolicitudesSinEscribirHistorico() throws Exception {
        SolicitudCambio abierta = cambios.crear(proyectoId, alta("Abierta", RiesgoSolicitud.CRITICO));
        abierta.finPrevisto = LocalDate.now().minusDays(3);
        solicitudes.save(abierta);
        etapa(abierta.id, 4, EstadoEtapa.BLOQUEADO);

        SolicitudCambio cerrada = cambios.crear(proyectoId, alta("Cerrada", RiesgoSolicitud.ALTO));
        cerrada.estado = EstadoSolicitud.CERRADO;
        cerrada.finPrevisto = LocalDate.now().minusDays(2);
        cerrada.finReal = LocalDate.now().minusDays(2);
        solicitudes.save(cerrada);
        etapa(cerrada.id, 10, EstadoEtapa.COMPLETADO);

        int snapshots = historicos.findByProyectoId(proyectoId).size();

        mvc.perform(get("/api/tablero/cambio/{id}", proyectoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteos.total").value(2))
                .andExpect(jsonPath("$.conteos.porEstado.ABIERTO").value(1))
                .andExpect(jsonPath("$.conteos.porEstado.CERRADO").value(1))
                .andExpect(jsonPath("$.conteos.atrasados").value(1))
                .andExpect(jsonPath("$.conteos.riesgoAlto").value(1))
                .andExpect(jsonPath("$.conteos.riesgoCritico").value(1))
                .andExpect(jsonPath("$.conteos.conBloqueo").value(1))
                .andExpect(jsonPath("$.conteos.adoptados").value(1))
                .andExpect(jsonPath("$.etapas.avancePromedio").value(5))
                .andExpect(jsonPath("$.alertas[*].tipo").value(hasItem(TableroCambio.ALERTA_ATRASO)))
                .andExpect(jsonPath("$.alertas[*].tipo").value(hasItem(TableroCambio.ALERTA_BLOQUEO)))
                .andExpect(jsonPath("$.alertas[*].tipo").value(hasItem(TableroCambio.ALERTA_RIESGO_CRITICO)));

        assertThat(historicos.findByProyectoId(proyectoId)).hasSize(snapshots);
    }

    @Test
    void trasElPasoKpiIncluyeLosKcDelHistorico() throws Exception {
        pasoKpi.ejecutar(proyectoId);

        mvc.perform(get("/api/tablero/cambio/{id}", proyectoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi.length()").value(13))
                .andExpect(jsonPath("$.kpi[0].codigo").value("KC-01"))
                .andExpect(jsonPath("$.kpi[0].categoria").value("CAMBIO"))
                .andExpect(jsonPath("$.kpi[0].noCalculable").value(true));
    }

    @Test
    void proyectoInexistenteEs404() throws Exception {
        mvc.perform(get("/api/tablero/cambio/{id}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(roles = "USUARIO_PROYECTO")
    void unUsuarioDeProyectoPuedeLeer() throws Exception {
        mvc.perform(get("/api/tablero/cambio/{id}", proyectoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proyectoId").value(proyectoId.intValue()));
    }

    @Test
    @WithAnonymousUser
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/tablero/cambio/{id}", proyectoId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    private void etapa(Long solicitudId, int numero, EstadoEtapa estado) {
        CambioEtapa e = etapas.findBySolicitudIdAndNumero(solicitudId, (short) numero).orElseThrow();
        e.estado = estado;
        etapas.save(e);
    }

    private static SolicitudCambioRequest alta(String nombre, RiesgoSolicitud riesgo) {
        return new SolicitudCambioRequest(
                nombre, null, null, null, null, null, null, null,
                null, TipoSolicitud.ALCANCE, riesgo, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
