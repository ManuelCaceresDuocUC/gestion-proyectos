package cl.proyecto.gestion.planificacion.lineabase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
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

/** A-14 por HTTP. Cuando C-01 active la seguridad, agregar @WithMockUser. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LineaBaseApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    Long proyectoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-API-B" + System.nanoTime() % 1_000_000, "API base", null,
            null, null, BigDecimal.ZERO, null, null, LocalDate.of(2026, 11, 2))).id;
        Long etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null)).id;
        tareas.crear(proyectoId, new TareaRequest(etapa, "A", null, 3, null, null));
    }

    @Test
    void fijaLaLineaBaseSinCuerpo() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/linea-base"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(jsonPath("$.motivo").value("Línea base v1"));
    }

    @Test
    void fijaConMotivoYLista() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/linea-base")
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Inicial\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/proyectos/" + proyectoId + "/lineas-base"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].motivo").value("Inicial"));
    }

    @Test
    void comparaConUnaFechaDeHoySimulada() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/linea-base")).andExpect(status().isCreated());
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion?hoy=2026-11-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionLineaBase").value(1))
            .andExpect(jsonPath("$.tareas.length()").value(2))
            .andExpect(jsonPath("$.tareas[1].atrasada").value(true)); // A termina el 4-nov y sigue al 0 %
    }

    @Test
    void unaFechaMalFormadaEs400() throws Exception {
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion?hoy=ayer"))
            .andExpect(status().isBadRequest());
    }
}
