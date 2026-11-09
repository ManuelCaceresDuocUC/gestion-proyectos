package cl.proyecto.gestion.cambio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** C-05: historial y evidencias por HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class TrazabilidadCambioApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-C05A-" + System.nanoTime() % 1_000_000, "API trazas",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @Test
    void historialVacioAlCrear() throws Exception {
        String id = crearSolicitud("Nueva");
        mvc.perform(get("/api/cambios/" + id + "/historial"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cambioDeEstadoYEtapaQuedanEnElHistorial() throws Exception {
        String id = crearSolicitud("Trazable");
        mvc.perform(put("/api/cambios/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Trazable\",\"tipo\":\"ALCANCE\",\"estado\":\"EN_EVALUACION\"}"))
            .andExpect(status().isOk());
        mvc.perform(patch("/api/cambios/" + id + "/etapas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_CURSO\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/cambios/" + id + "/historial"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].campo").value("estado"))
            .andExpect(jsonPath("$[0].valorAnterior").value("ABIERTO"))
            .andExpect(jsonPath("$[0].valorNuevo").value("EN_EVALUACION"))
            .andExpect(jsonPath("$[1].campo").value("etapa.1.estado"))
            .andExpect(jsonPath("$[1].valorAnterior").value("PENDIENTE"))
            .andExpect(jsonPath("$[1].valorNuevo").value("EN_CURSO"));
    }

    @Test
    void historialInexistenteEs404() throws Exception {
        mvc.perform(get("/api/cambios/-1/historial"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void creaListaYBorraEvidencia() throws Exception {
        String id = crearSolicitud("Con prueba");
        mvc.perform(post("/api/cambios/" + id + "/evidencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcion\":\"Acta del comité\",\"url\":\"https://ejemplo.cl/acta\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.descripcion").value("Acta del comité"))
            .andExpect(jsonPath("$.url").value("https://ejemplo.cl/acta"))
            .andExpect(jsonPath("$.solicitudId").isNumber())
            .andExpect(jsonPath("$.creadoEn").isNotEmpty());

        String body = mvc.perform(get("/api/cambios/" + id + "/evidencias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].descripcion").value("Acta del comité"))
            .andReturn().getResponse().getContentAsString();
        String evidenciaId = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(delete("/api/cambios/" + id + "/evidencias/" + evidenciaId))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/cambios/" + id + "/evidencias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void urlEnBlancoQuedaNula() throws Exception {
        String id = crearSolicitud("Sin url");
        mvc.perform(post("/api/cambios/" + id + "/evidencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcion\":\"Foto\",\"url\":\"  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").value((Object) null));
    }

    @Test
    void sinDescripcionEs400() throws Exception {
        String id = crearSolicitud("Vacia");
        mvc.perform(post("/api/cambios/" + id + "/evidencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://ejemplo.cl\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void evidenciaDeOtraSolicitudEs404() throws Exception {
        String una = crearSolicitud("A");
        String otra = crearSolicitud("B");
        String body = mvc.perform(post("/api/cambios/" + una + "/evidencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcion\":\"Solo de A\"}"))
            .andReturn().getResponse().getContentAsString();
        String evidenciaId = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(delete("/api/cambios/" + otra + "/evidencias/" + evidenciaId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void proyectoCerradoNoAceptaEvidencias() throws Exception {
        String id = crearSolicitud("Tarde");
        proyectos.cerrar(proyectoId);
        mvc.perform(post("/api/cambios/" + id + "/evidencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcion\":\"Tarde\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
        mvc.perform(get("/api/cambios/" + id + "/evidencias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/cambios/" + id + "/historial"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO_PROYECTO")
    void usuarioProyectoPuedeCargarEvidencia() throws Exception {
        String id = crearSolicitud("Usuario");
        mvc.perform(post("/api/cambios/" + id + "/evidencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcion\":\"Adjunto del usuario\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.descripcion").value("Adjunto del usuario"));
    }

    private String crearSolicitud(String nombre) throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"tipo\":\"ALCANCE\"}"))
            .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
    }
}
