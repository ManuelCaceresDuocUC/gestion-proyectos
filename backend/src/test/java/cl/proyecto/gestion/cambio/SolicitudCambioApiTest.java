package cl.proyecto.gestion.cambio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** C-03 por HTTP. Cuando C-01 active la seguridad, agregar @WithMockUser. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitudCambioApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-C03A-" + System.nanoTime() % 1_000_000, "API cambio",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @Test
    void creaYConsulta() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ampliar plazo\",\"tipo\":\"TIEMPO\",\"finPrevisto\":\"2026-11-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("SC-01"))
            .andExpect(jsonPath("$.estado").value("ABIERTO"))
            .andExpect(jsonPath("$.aplicada").value(false))
            .andExpect(jsonPath("$.tipo").value("TIEMPO"))
            .andExpect(jsonPath("$.diasAtraso").isNumber());
    }

    @Test
    void listaConFiltroDeTipo() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Tiempo\",\"tipo\":\"TIEMPO\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Costo\",\"tipo\":\"COSTO\"}"))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/proyectos/" + proyectoId + "/cambios").param("tipo", "COSTO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nombre").value("Costo"));
    }

    @Test
    void inexistenteEs404() throws Exception {
        mvc.perform(get("/api/cambios/-1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void sinNombreEs400() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"TIEMPO\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void tipoInvalidoEs400() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"tipo\":\"EN_CURSO\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void proyectoCerradoNoAceptaAltas() throws Exception {
        proyectos.cerrar(proyectoId);
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Tarde\",\"tipo\":\"ALCANCE\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
    }

    @Test
    void editaYElimina() throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Original\",\"tipo\":\"TIEMPO\"}"))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(put("/api/cambios/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Editada\",\"tipo\":\"TIEMPO\",\"estado\":\"EN_EVALUACION\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Editada"))
            .andExpect(jsonPath("$.estado").value("EN_EVALUACION"))
            .andExpect(jsonPath("$.codigo").value("SC-01"));

        mvc.perform(delete("/api/cambios/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/cambios/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void diasAtrasoEnLaRespuesta() throws Exception {
        LocalDate fin = LocalDate.now().minusDays(5);
        mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Atrasada\",\"tipo\":\"TIEMPO\",\"finPrevisto\":\"" + fin + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.diasAtraso").value(5));
    }
}
