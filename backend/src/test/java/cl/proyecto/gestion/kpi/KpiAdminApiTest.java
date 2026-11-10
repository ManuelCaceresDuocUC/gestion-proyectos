package cl.proyecto.gestion.kpi;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** B-15: alta, edición y baja de definiciones. No calcula ni escribe histórico. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class KpiAdminApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired KpiHistoricoRepository historicos;

    @Test
    void creaEditaYNoEscribeHistorico() throws Exception {
        Proyecto proyecto = proyectos.crear(req("B15-KPI"));
        String alta = cuerpo(proyecto.id, "B15-A", true, "AVANCE_REAL / ACTIVIDADES_TOTAL", "MAYOR_IGUAL", "80", null);

        String creado = mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alta))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("B15-A"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.variables", containsInAnyOrder("AVANCE_REAL", "ACTIVIDADES_TOTAL")))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(creado.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mvc.perform(get("/api/kpi/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formula").value("AVANCE_REAL / ACTIVIDADES_TOTAL"));

        String edicion = cuerpo(proyecto.id, "B15-A", false, "ACTIVIDADES_ATRASADAS", "MENOR_IGUAL", "10", null);
        mvc.perform(put("/api/kpi/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.sentido").value("MENOR_MEJOR"))
                .andExpect(jsonPath("$.variables", hasItem("ACTIVIDADES_ATRASADAS")));

        mvc.perform(get("/api/kpi").param("proyectoId", proyecto.id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'B15-A')].activo").value(hasItem(false)));
        mvc.perform(get("/api/proyectos/" + proyecto.id + "/kpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'B15-A')]").isEmpty());
        org.assertj.core.api.Assertions.assertThat(historicos.countByKpiId(id)).isZero();
    }

    @Test
    void rechazaFormulaVariableYEntreSinMetaMax() throws Exception {
        Proyecto proyecto = proyectos.crear(req("B15-VAL"));

        mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(proyecto.id, "B15-B", true, "AVANCE_REAL /", "MAYOR_IGUAL", "1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FORMULA_INVALIDA"));

        mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(proyecto.id, "B15-C", true, "CAMBIOS_TOTAL", "MAYOR_IGUAL", "1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VARIABLE_DESCONOCIDA"));

        mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(proyecto.id, "B15-D", true, "AVANCE_REAL", "ENTRE", "1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("META_MAX_OBLIGATORIA"));

        mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(proyecto.id, "B15-E", true, "AVANCE_REAL", "ENTRE", "1", "2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operadorMeta").value("ENTRE"))
                .andExpect(jsonPath("$.metaMax").value(2));
    }

    @Test
    void codigoDuplicadoYProyectoCerrado() throws Exception {
        Proyecto proyecto = proyectos.crear(req("B15-DUP"));
        String alta = cuerpo(proyecto.id, "B15-F", true, "AVANCE_REAL", "MAYOR_IGUAL", "1", null);
        mvc.perform(post("/api/kpi").contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/kpi").contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_DUPLICADO"));

        proyectos.cerrar(proyecto.id);
        mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(proyecto.id, "B15-G", true, "AVANCE_REAL", "MAYOR_IGUAL", "1", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROYECTO_CERRADO"));
    }

    @Test
    void eliminaLaDefinicion() throws Exception {
        Proyecto proyecto = proyectos.crear(req("B15-DEL"));
        String creado = mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(proyecto.id, "B15-H", true, "AVANCE_REAL", "MAYOR_IGUAL", "1", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(creado.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mvc.perform(delete("/api/kpi/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/kpi/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USUARIO_PROYECTO")
    void usuarioDeProyectoNoEscribe() throws Exception {
        mvc.perform(post("/api/kpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(null, "B15-I", true, "AVANCE_REAL", "MAYOR_IGUAL", "1", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    private static ProyectoRequest req(String codigo) {
        return new ProyectoRequest(codigo, "Proyecto " + codigo, null, null, null,
                new BigDecimal("1000000"), null, null, null);
    }

    private static String cuerpo(
            Long proyectoId, String codigo, boolean activo, String formula,
            String operador, String meta, String metaMax) {
        String proyecto = proyectoId == null ? "null" : proyectoId.toString();
        String max = metaMax == null ? "null" : metaMax;
        return """
                {
                  "proyectoId": %s,
                  "codigo": "%s",
                  "nombre": "KPI de prueba",
                  "categoria": "PROYECTO",
                  "formula": "%s",
                  "unidad": "%%",
                  "sentido": "%s",
                  "operadorMeta": "%s",
                  "meta": %s,
                  "metaMax": %s,
                  "umbralVerde": 1,
                  "umbralAmarillo": 0.8,
                  "responsable": "Coordinación",
                  "activo": %s
                }
                """.formatted(
                proyecto, codigo, formula,
                "ENTRE".equals(operador) || "MAYOR_IGUAL".equals(operador) ? "MAYOR_MEJOR" : "MENOR_MEJOR",
                operador, meta, max, activo);
    }
}
