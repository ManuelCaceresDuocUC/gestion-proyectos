package cl.proyecto.gestion.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/**
 * A-18, recálculo 1 de la demo: cambiar la duración de una tarea con sucesoras de los cuatro tipos de dependencia.
 * Se verifica por HTTP, igual que lo hace la interfaz: se fija la línea base, se cambia la duración con PATCH y se
 * lee la comparación contra la base. Las desviaciones están en días hábiles (positivo = más tarde que la base).
 * Caso documentado en docs/pruebas/A-18-recalculo-duracion.md.
 *
 * Escenario (inicio lunes 2-nov-2026):
 *   1.1 A (5 d) -FS-> 1.2 B (2 d)   B empieza cuando A termina
 *   1.1 A       -SS-> 1.3 C (3 d)   C empieza con A (+1 d de desfase)
 *   1.1 A       -FF-> 1.4 D (4 d)   D termina cuando A termina
 *   1.1 A       -SF-> 1.5 E (3 d)   E termina cuando A empieza
 * Al pasar A de 5 a 7 días: A termina 2 días hábiles más tarde, y con ella B (FS) y D (FF).
 * C (SS) y E (SF) dependen del inicio de A, que no cambia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecalculoDuracionE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    Long proyectoId;
    Long a, b, c, d, e;

    @BeforeEach
    void escenario() throws Exception {
        proyectoId = proyectos.crear(new ProyectoRequest("T-E2E" + System.nanoTime() % 1_000_000, "E2E duración", null,
            null, null, BigDecimal.ZERO, null, null, LocalDate.of(2026, 11, 2))).id;
        Long etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null)).id;
        a = tareas.crear(proyectoId, new TareaRequest(etapa, "A", null, 5, null, null)).id;
        b = tareas.crear(proyectoId, new TareaRequest(etapa, "B", null, 2, null, null)).id;
        c = tareas.crear(proyectoId, new TareaRequest(etapa, "C", null, 3, null, null)).id;
        d = tareas.crear(proyectoId, new TareaRequest(etapa, "D", null, 4, null, null)).id;
        e = tareas.crear(proyectoId, new TareaRequest(etapa, "E", null, 3, null, null)).id;
        dependencia(a, b, "FS", 0).andExpect(status().isCreated());
        dependencia(a, c, "SS", 1).andExpect(status().isCreated());
        dependencia(a, d, "FF", 0).andExpect(status().isCreated());
        dependencia(a, e, "SF", 0).andExpect(status().isCreated());
        mvc.perform(post("/api/proyectos/" + proyectoId + "/linea-base")).andExpect(status().isCreated());
    }

    @Test
    void cambiarLaDuracionMueveSoloLasSucesorasQueDependenDelFin() throws Exception {
        mvc.perform(patch("/api/tareas/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"duracionDias\":7}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cambios[?(@.codigo=='1.1')]").isNotEmpty())  // A
            .andExpect(jsonPath("$.cambios[?(@.codigo=='1.2')]").isNotEmpty())  // B (FS)
            .andExpect(jsonPath("$.cambios[?(@.codigo=='1.4')]").isNotEmpty())  // D (FF)
            .andExpect(jsonPath("$.cambios[?(@.codigo=='1.3')]").isEmpty())     // C (SS) no cambia
            .andExpect(jsonPath("$.cambios[?(@.codigo=='1.5')]").isEmpty());    // E (SF) no cambia

        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionLineaBase").value(1))
            // A: mismo inicio, termina 2 días hábiles después
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].desviacionInicioDias").value(0))
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].desviacionFinDias").value(2))
            // B (FS): empieza y termina 2 días después
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.2')].desviacionInicioDias").value(2))
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.2')].desviacionFinDias").value(2))
            // C (SS) y E (SF) no se mueven
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.3')].desviacionFinDias").value(0))
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.5')].desviacionFinDias").value(0))
            // D (FF): termina 2 días después
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.4')].desviacionFinDias").value(2))
            // el fin del proyecto lo marca B y se corre 2 días hábiles; la etapa lo refleja
            .andExpect(jsonPath("$.resumen.desviacionFinDias").value(2))
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1')].desviacionFinDias").value(2));
    }

    @Test
    void laLineaBaseNoSeMueveConElRecalculo() throws Exception {
        String antes = mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
            .andReturn().getResponse().getContentAsString();
        mvc.perform(patch("/api/tareas/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"duracionDias\":7}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
            .andExpect(jsonPath("$.tareas[?(@.codigo=='1.1')].finBase").value(
                com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(antes, "$.tareas[?(@.codigo=='1.1')].finBase").get(0)))
            .andExpect(jsonPath("$.resumen.finBase").value(
                com.jayway.jsonpath.JsonPath.<String>read(antes, "$.resumen.finBase")));
    }

    @Test
    void volverALaDuracionOriginalDejaTodoSinDesviacion() throws Exception {
        mvc.perform(patch("/api/tareas/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"duracionDias\":7}"))
            .andExpect(status().isOk());
        mvc.perform(patch("/api/tareas/" + a).contentType(MediaType.APPLICATION_JSON).content("{\"duracionDias\":5}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
            .andExpect(jsonPath("$.resumen.desviacionFinDias").value(0));
    }

    @Test
    void unCicloSeRechazaYNoCambiaNingunaFecha() throws Exception {
        dependencia(b, a, "FS", 0)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CICLO_EN_DEPENDENCIAS"));
        mvc.perform(get("/api/proyectos/" + proyectoId + "/comparacion"))
            .andExpect(jsonPath("$.resumen.desviacionFinDias").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions dependencia(Long pred, Long suc, String tipo, int desfase)
            throws Exception {
        String cuerpo = "{\"predecesoraId\":" + pred + ",\"sucesoraId\":" + suc + ",\"tipo\":\"" + tipo
            + "\",\"desfaseDias\":" + desfase + "}";
        return mvc.perform(post("/api/proyectos/" + proyectoId + "/dependencias")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
