package cl.proyecto.gestion.cambio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** C-03 / C-08 / C-09 por HTTP. Cuando C-01 active la seguridad, agregar @WithMockUser. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitudCambioApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

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
    void putConSaltoIlegalEs409() throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Original\",\"tipo\":\"TIEMPO\"}"))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(put("/api/cambios/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Original\",\"tipo\":\"TIEMPO\",\"estado\":\"APROBADO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void aprobarYRechazarPorHttp() throws Exception {
        String idOk = crearYPasarAEvaluacion("Para aprobar");
        mvc.perform(post("/api/cambios/" + idOk + "/aprobar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("APROBADO"))
            .andExpect(jsonPath("$.aprobadaEn").isNotEmpty())
            .andExpect(jsonPath("$.aplicada").value(false));

        String idNo = crearYPasarAEvaluacion("Para rechazar");
        mvc.perform(post("/api/cambios/" + idNo + "/rechazar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECHAZADO"))
            .andExpect(jsonPath("$.aprobadaEn").value((Object) null));
    }

    @Test
    void aprobarEnProyectoCerradoEs409() throws Exception {
        String id = crearYPasarAEvaluacion("Cerrado");
        proyectos.cerrar(proyectoId);
        mvc.perform(post("/api/cambios/" + id + "/aprobar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
    }

    @Test
    void aprobarInexistenteEs404() throws Exception {
        mvc.perform(post("/api/cambios/-1/aprobar"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void aprobarAbiertaEs409() throws Exception {
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Abierta\",\"tipo\":\"TIEMPO\"}"))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(post("/api/cambios/" + id + "/aprobar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void aplicarTiempoPorHttpMueveDuracion() throws Exception {
        Tarea etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null));
        Tarea hoja = tareas.crear(proyectoId, new TareaRequest(etapa.id, "Hoja", null, 5, null, null));
        String id = crearAprobada("Plazo", "TIEMPO", hoja.id, 3);

        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aplicada").value(true))
            .andExpect(jsonPath("$.aplicadaEn").isNotEmpty())
            .andExpect(jsonPath("$.estado").value("EN_IMPLEMENTACION"));

        mvc.perform(get("/api/tareas/" + hoja.id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.duracionDias").value(8));
    }

    @Test
    void aplicarDosVecesEs409() throws Exception {
        String id = crearAprobada("Costo", "COSTO", null, null);
        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aplicada").value(true));
        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("YA_APLICADA"));
    }

    @Test
    void aplicarSinAprobarEs409() throws Exception {
        String id = crearYPasarAEvaluacion("AunNo");
        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void aplicarTiempoSinTareaEs400() throws Exception {
        String id = crearAprobada("SinTarea", "TIEMPO", null, 2);
        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("TAREA_REQUERIDA"));
    }

    @Test
    void aplicarInexistenteEs404() throws Exception {
        mvc.perform(post("/api/cambios/-1/aplicar"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void aplicarEnProyectoCerradoEs409() throws Exception {
        String id = crearAprobada("Tarde", "COSTO", null, null);
        proyectos.cerrar(proyectoId);
        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
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

    private String crearYPasarAEvaluacion(String nombre) throws Exception {
        return crearYPasarAEvaluacion(nombre, "TIEMPO", null, null);
    }

    private String crearAprobada(String nombre, String tipo, Long tareaId, Integer impactoDias) throws Exception {
        String id = crearYPasarAEvaluacion(nombre, tipo, tareaId, impactoDias);
        mvc.perform(post("/api/cambios/" + id + "/aprobar")).andExpect(status().isOk());
        return id;
    }

    private String crearYPasarAEvaluacion(String nombre, String tipo, Long tareaId, Integer impactoDias)
            throws Exception {
        String extra = "";
        if (tareaId != null) {
            extra += ",\"tareaId\":" + tareaId;
        }
        if (impactoDias != null) {
            extra += ",\"impactoDias\":" + impactoDias;
        }
        String datos = "{\"nombre\":\"" + nombre + "\",\"tipo\":\"" + tipo + "\"" + extra + "}";
        String body = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(datos))
            .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(put("/api/cambios/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"tipo\":\"" + tipo + "\",\"estado\":\"EN_EVALUACION\""
                    + extra + "}"))
            .andExpect(status().isOk());
        return id;
    }
}
