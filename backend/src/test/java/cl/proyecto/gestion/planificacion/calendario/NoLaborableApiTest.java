package cl.proyecto.gestion.planificacion.calendario;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class NoLaborableApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
            "T-B07-" + System.nanoTime() % 1_000_000,
            "Calendario",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            "LMXJV",
            LocalDate.of(2026, 11, 2))).id;
    }

    @Test
    void administraFeriadoDeProyectoYConsultaLosGenerales() throws Exception {
        mvc.perform(post("/api/no-laborables")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proyectoId\":null,\"fecha\":\"2026-11-03\",\"descripcion\":\"Feriado general\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.proyectoId").doesNotExist());

        mvc.perform(post("/api/no-laborables")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proyectoId\":" + proyectoId
                    + ",\"fecha\":\"2026-11-04\",\"descripcion\":\"Feriado del proyecto\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.proyectoId").value(proyectoId));

        mvc.perform(get("/api/no-laborables?proyectoId=" + proyectoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rechazaFechaDuplicada() throws Exception {
        String cuerpo = "{\"proyectoId\":" + proyectoId
            + ",\"fecha\":\"2026-11-04\",\"descripcion\":\"Feriado\"}";

        mvc.perform(post("/api/no-laborables")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/no-laborables")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("DIA_NO_LABORABLE_DUPLICADO"));
    }

    @Test
    void actualizaYEliminaFeriado() throws Exception {
        String respuesta = mvc.perform(post("/api/no-laborables")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proyectoId\":" + proyectoId
                    + ",\"fecha\":\"2026-11-04\",\"descripcion\":\"Feriado\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(put("/api/no-laborables/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proyectoId\":" + proyectoId
                    + ",\"fecha\":\"2026-11-05\",\"descripcion\":\"Actualizado\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fecha").value("2026-11-05"));

        mvc.perform(delete("/api/no-laborables/" + id))
            .andExpect(status().isNoContent());
    }
}
