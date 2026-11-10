package cl.proyecto.gestion.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.kpi.CatalogoKpiCambio;
import cl.proyecto.gestion.kpi.CatalogoKpiProyecto;
import cl.proyecto.gestion.kpi.KpiHistoricoRepository;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-14, recálculo 3 de la demo: aprobar no mueve el plan; aplicar una solicitud TIEMPO sí.
 * Se verifica por HTTP, igual que la interfaz. Caso: docs/pruebas/casos-de-prueba.md (CP-03).
 *
 * Escenario (inicio lunes 2-nov-2026):
 *   1.1 A (5 d) -FS-> 1.2 B (2 d)
 * Línea base v1. Solicitud TIEMPO sobre A con impacto +3.
 * Aprobar: duración 5, desviación 0, mismo snapshot KPI.
 * Aplicar: A pasa a 8 d; B (FS) se corre; finBase igual; costos plan iguales; KPI y tableros leen el recálculo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class RecalculoSolicitudE2ETest {

    private static final int CATALOGO =
            CatalogoKpiProyecto.todos().size() + CatalogoKpiCambio.todos().size();

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired AsignacionService asignaciones;
    @Autowired KpiHistoricoRepository historicos;

    Long proyectoId;
    Long a;
    Long b;

    @BeforeEach
    void escenario() throws Exception {
        proyectoId = proyectos.crear(new ProyectoRequest(
                "T-C14-" + System.nanoTime() % 1_000_000,
                "E2E solicitud",
                null, null, null,
                new BigDecimal("2000.00"), "CLP", null,
                LocalDate.of(2026, 11, 2))).id;
        Long etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null)).id;
        a = tareas.crear(proyectoId, new TareaRequest(etapa, "A", null, 5, null, null)).id;
        b = tareas.crear(proyectoId, new TareaRequest(etapa, "B", null, 2, null, null)).id;
        dependencia(a, b, "FS", 0).andExpect(status().isCreated());

        var tipo = tipos.crear(new TipoRecursoRequest("Prof-" + System.nanoTime(), "hora"));
        var recurso = recursos.crear(new RecursoRequest(
                tipo.id, "Ana", new BigDecimal("100.00"), new BigDecimal("8.0"), true));
        asignaciones.crear(new AsignacionRequest(
                a, recurso.id, new BigDecimal("15.00"), null, null, null));

        mvc.perform(post("/api/proyectos/" + proyectoId + "/linea-base")).andExpect(status().isCreated());
    }

    @Test
    void aprobarNoMueveYAplicarTiempoMuevePlanKpiYTableros() throws Exception {
        String finBaseA = campoTarea(a, "$.finBase");
        String finPlanA = campoTarea(a, "$.finPlan");
        int snapshots = historicos.findByProyectoId(proyectoId).size();
        double costoPlan = JsonPath.read(
                mvc.perform(get("/api/tablero/proyecto/" + proyectoId)).andReturn().getResponse().getContentAsString(),
                "$.costos.plan");

        String id = crearEnEvaluacion("Ampliar plazo", a, 3);

        mvc.perform(get("/api/tareas/" + a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionDias").value(5))
                .andExpect(jsonPath("$.finBase").value(finBaseA))
                .andExpect(jsonPath("$.finPlan").value(finPlanA));

        mvc.perform(post("/api/cambios/" + id + "/aprobar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.aplicada").value(false));

        mvc.perform(get("/api/tareas/" + a))
                .andExpect(jsonPath("$.duracionDias").value(5))
                .andExpect(jsonPath("$.finBase").value(finBaseA))
                .andExpect(jsonPath("$.finPlan").value(finPlanA));
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionLineaBase").value(1))
                .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].desviacionFinDias").value(0))
                .andExpect(jsonPath("$.resumen.desviacionFinDias").value(0));
        org.assertj.core.api.Assertions.assertThat(historicos.findByProyectoId(proyectoId)).hasSize(snapshots);

        mvc.perform(post("/api/cambios/" + id + "/aplicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(true))
                .andExpect(jsonPath("$.estado").value("EN_IMPLEMENTACION"));

        mvc.perform(get("/api/tareas/" + a))
                .andExpect(jsonPath("$.duracionDias").value(8))
                .andExpect(jsonPath("$.finBase").value(finBaseA));
        mvc.perform(get("/api/tareas/" + b))
                .andExpect(jsonPath("$.duracionDias").value(2));
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
                .andExpect(jsonPath("$.versionLineaBase").value(1))
                .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].desviacionInicioDias").value(0))
                .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].desviacionFinDias").value(3))
                .andExpect(jsonPath("$.tareas[?(@.codigo=='1.2')].desviacionInicioDias").value(3))
                .andExpect(jsonPath("$.tareas[?(@.codigo=='1.2')].desviacionFinDias").value(3))
                .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].finBase").value(Matchers.hasItem(finBaseA)))
                .andExpect(jsonPath("$.resumen.desviacionFinDias").value(3));

        org.assertj.core.api.Assertions.assertThat(historicos.findByProyectoId(proyectoId))
                .hasSize(snapshots + CATALOGO);

        mvc.perform(get("/api/proyectos/" + proyectoId + "/kpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(CATALOGO))
                .andExpect(jsonPath("$[?(@.codigo=='KP-01')].motivo", Matchers.hasItem("RECALCULO")))
                .andExpect(jsonPath("$[?(@.codigo=='KC-01')].categoria", Matchers.hasItem("CAMBIO")));

        mvc.perform(get("/api/tablero/proyecto/" + proyectoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costos.plan").value(costoPlan))
                .andExpect(jsonPath("$.kpi[*].codigo", Matchers.hasItem("KP-01")))
                .andExpect(jsonPath("$.kpi[*].categoria", Matchers.not(Matchers.hasItem("CAMBIO"))));

        mvc.perform(get("/api/tablero/cambio/" + proyectoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteos.total").value(1))
                .andExpect(jsonPath("$.conteos.porEstado.EN_IMPLEMENTACION").value(1))
                .andExpect(jsonPath("$.conteos.porEstado.APROBADO").value(0))
                .andExpect(jsonPath("$.kpi.length()").value(CatalogoKpiCambio.todos().size()))
                .andExpect(jsonPath("$.kpi[0].codigo").value("KC-01"));
    }

    @Test
    @WithMockUser(roles = "USUARIO_PROYECTO")
    void unUsuarioDeProyectoNoAprueba() throws Exception {
        String id = crearEnEvaluacion("Sin permiso", a, 3);
        mvc.perform(post("/api/cambios/" + id + "/aprobar"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
        mvc.perform(get("/api/tareas/" + a))
                .andExpect(jsonPath("$.duracionDias").value(5));
    }

    private String crearEnEvaluacion(String nombre, Long tareaId, int impacto) throws Exception {
        String alta = "{\"nombre\":\"" + nombre + "\",\"tipo\":\"TIEMPO\",\"tareaId\":" + tareaId
                + ",\"impactoDias\":" + impacto + "}";
        String creado = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                        .contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(creado, "$.id");
        String edicion = "{\"nombre\":\"" + nombre + "\",\"tipo\":\"TIEMPO\",\"estado\":\"EN_EVALUACION\",\"tareaId\":"
                + tareaId + ",\"impactoDias\":" + impacto + "}";
        mvc.perform(put("/api/cambios/" + id).contentType(MediaType.APPLICATION_JSON).content(edicion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_EVALUACION"));
        return String.valueOf(id);
    }

    private String campoTarea(Long tareaId, String ruta) throws Exception {
        String cuerpo = mvc.perform(get("/api/tareas/" + tareaId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, ruta);
    }

    private org.springframework.test.web.servlet.ResultActions dependencia(Long pred, Long suc, String tipo, int desfase)
            throws Exception {
        String cuerpo = "{\"predecesoraId\":" + pred + ",\"sucesoraId\":" + suc + ",\"tipo\":\"" + tipo
                + "\",\"desfaseDias\":" + desfase + "}";
        return mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
