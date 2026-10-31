package cl.proyecto.gestion.planificacion.dependencia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
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

/** A-09/A-13 por HTTP. Cuando C-01 active la seguridad, agregar @WithMockUser. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DependenciaApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    Long proyectoId;
    Tarea a;
    Tarea b;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-API-D" + System.nanoTime() % 1_000_000, "API", null,
            null, null, BigDecimal.ZERO, null, null, LocalDate.of(2026, 11, 2))).id;
        Tarea etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null));
        a = tareas.crear(proyectoId, new TareaRequest(etapa.id, "A", null, 3, null, null));
        b = tareas.crear(proyectoId, new TareaRequest(etapa.id, "B", null, 2, null, null));
    }

    String cuerpo(Tarea x, Tarea y, String tipo) {
        return "{\"predecesoraId\":" + x.id + ",\"sucesoraId\":" + y.id + ",\"tipo\":\"" + tipo + "\",\"desfaseDias\":0}";
    }

    @Test
    void creaListaYEliminaUnaDependencia() throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(a, b, "FS")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dependencia.tipo").value("FS"))
            .andExpect(jsonPath("$.cambios").isArray())
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"dependencia\":\\{\"id\":(\\d+).*", "$1");
        mvc.perform(get("/api/proyectos/" + proyectoId + "/dependencias"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].sucesoraId").value(b.id));
        mvc.perform(delete("/api/dependencias/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.cambios").isArray());
    }

    @Test
    void elCicloResponde409ConUnMensajeClaro() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(a, b, "FS")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(b, a, "SS")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CICLO_EN_DEPENDENCIAS"))
            .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("1.1 A")));
    }

    @Test
    void tipoInvalidoEs400() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(a, b, "XX")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void patchDeDuracionRespondeConLasFechasAfectadas() throws Exception {
        mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(a, b, "FS")))
            .andExpect(status().isCreated());
        mvc.perform(patch("/api/tareas/" + a.id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"duracionDias\":6}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tarea.duracionDias").value(6))
            .andExpect(jsonPath("$.cambios[?(@.codigo=='1.2')].inicioDespues").value("2026-11-10"));
    }

    @Test
    void patchVacioEs400() throws Exception {
        mvc.perform(patch("/api/tareas/" + a.id).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("PATCH_VACIO"));
    }
}
