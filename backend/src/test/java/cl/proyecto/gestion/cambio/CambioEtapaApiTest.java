package cl.proyecto.gestion.cambio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** C-04 por HTTP. Cuando C-01 active la seguridad, agregar @WithMockUser. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CambioEtapaApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-C04A-" + System.nanoTime() % 1_000_000, "API etapas",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @Test
    void altaTraeAvanceCeroYDiezEtapas() throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Con etapas\",\"tipo\":\"TIEMPO\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.avance").value(0))
            .andExpect(jsonPath("$.etapasCompletas").value(0))
            .andExpect(jsonPath("$.etapasBloqueadas").value(0))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(get("/api/cambios/" + id + "/etapas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapas.length()").value(10))
            .andExpect(jsonPath("$.etapas[0].numero").value(1))
            .andExpect(jsonPath("$.etapas[0].nombre").value("Necesidad"))
            .andExpect(jsonPath("$.etapas[0].estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.etapas[9].numero").value(10))
            .andExpect(jsonPath("$.avance").value(0));
    }

    @Test
    void patchCambiaEstadoYSubeAvance() throws Exception {
        String id = crearSolicitud();

        mvc.perform(patch("/api/cambios/" + id + "/etapas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"COMPLETADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero").value(1))
            .andExpect(jsonPath("$.estado").value("COMPLETADO"));

        mvc.perform(get("/api/cambios/" + id + "/etapas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etapasCompletas").value(1))
            .andExpect(jsonPath("$.avance").value(10))
            .andExpect(jsonPath("$.etapas[0].estado").value("COMPLETADO"));

        mvc.perform(get("/api/cambios/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avance").value(10));
    }

    @Test
    void etapaInexistenteEs404() throws Exception {
        String id = crearSolicitud();
        mvc.perform(patch("/api/cambios/" + id + "/etapas/11")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_CURSO\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void sinEstadoEs400() throws Exception {
        String id = crearSolicitud();
        mvc.perform(patch("/api/cambios/" + id + "/etapas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void proyectoCerradoNoAceptaPatch() throws Exception {
        String id = crearSolicitud();
        proyectos.cerrar(proyectoId);
        mvc.perform(patch("/api/cambios/" + id + "/etapas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"EN_CURSO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
    }

    private String crearSolicitud() throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"C-04\",\"tipo\":\"ALCANCE\"}"))
            .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
    }
}
