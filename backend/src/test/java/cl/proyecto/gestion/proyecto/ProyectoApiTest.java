package cl.proyecto.gestion.proyecto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** A-04/A-05 por HTTP. Cuando C-01 active la seguridad, agregar @WithMockUser a estos tests. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProyectoApiTest {

    @Autowired MockMvc mvc;

    static String json(String codigo, String presupuesto) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"API\",\"presupuesto\":" + presupuesto + "}";
    }

    @Test
    void creaYConsulta() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content(json("T-API-1", "100")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("T-API-1"))
            .andExpect(jsonPath("$.estado").value("PLANIFICADO"));
    }

    @Test
    void presupuestoNegativoEs400() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content(json("T-API-2", "-5")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void codigoDuplicadoEs409() throws Exception {
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content(json("T-API-3", "1")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON).content(json("T-API-3", "1")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));
    }

    @Test
    void inexistenteEs404() throws Exception {
        mvc.perform(get("/api/proyectos/-1")).andExpect(status().isNotFound());
    }

    @Test
    void cerradoNoSeEdita() throws Exception {
        String body = mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
                .content(json("T-API-4", "1")))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(post("/api/proyectos/" + id + "/cerrar")).andExpect(status().isOk());
        mvc.perform(put("/api/proyectos/" + id).contentType(MediaType.APPLICATION_JSON).content(json("T-API-4", "2")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
    }

    @Test
    void edtPorHttp() throws Exception {
        String body = mvc.perform(post("/api/proyectos").contentType(MediaType.APPLICATION_JSON)
                .content(json("T-API-5", "1")))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(post("/api/proyectos/" + id + "/tareas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Etapa\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo").value("1"))
            .andExpect(jsonPath("$.tipo").value("ETAPA"));
        mvc.perform(get("/api/proyectos/" + id + "/edt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hijos").isArray());
    }
}
