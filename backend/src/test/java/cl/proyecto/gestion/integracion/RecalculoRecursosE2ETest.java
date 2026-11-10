package cl.proyecto.gestion.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.kpi.VariablesProyecto;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecurso;
import cl.proyecto.gestion.recurso.TipoRecursoRepository;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
 * B-18, recálculo 2 de la demo: bajar las horas de una asignación y leer costos, alerta,
 * KPI y tablero de proyecto por HTTP. Caso documentado en docs/pruebas/casos-de-prueba.md (CP-02).
 *
 * Hoja de 5 días hábiles, profesional tarifa 32.000 y capacidad 8. 45 h son 9 h/día (alerta);
 * 40 h son 8 h/día (la alerta desaparece porque la carga no supera la capacidad).
 * KP-03 queda «No calculable»: el costo real sigue en 0 porque la API no recibe cantidadReal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class RecalculoRecursosE2ETest {

    private static final BigDecimal COSTO_45 = new BigDecimal("1440000.00");
    private static final BigDecimal COSTO_40 = new BigDecimal("1280000.00");
    private static final BigDecimal CERO = new BigDecimal("0");

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired TipoRecursoService tipos;
    @Autowired TipoRecursoRepository tiposRepo;
    @Autowired RecursoService recursos;

    Long proyectoId;
    Long hojaId;
    Long recursoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
            "T-E2E" + System.nanoTime() % 1_000_000,
            "E2E recursos",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            null,
            LocalDate.of(2026, 11, 2))).id;
        Long etapa = tareas.crear(proyectoId, new TareaRequest(null, "Etapa", null, null, null, null)).id;
        Long actividad = tareas.crear(proyectoId, new TareaRequest(etapa, "Actividad", null, null, null, null)).id;
        hojaId = tareas.crear(proyectoId, new TareaRequest(actividad, "Hoja", null, 5, null, null)).id;

        TipoRecurso profesional = tiposRepo.findByNombreIgnoreCase(VariablesProyecto.TIPO_PROFESIONAL)
            .orElseGet(() -> tipos.crear(new TipoRecursoRequest(VariablesProyecto.TIPO_PROFESIONAL, "hora")));
        recursoId = recursos.crear(new RecursoRequest(
            profesional.id,
            "P2 E2E " + System.nanoTime(),
            new BigDecimal("32000"),
            new BigDecimal("8"),
            true)).id;
    }

    @Test
    void bajarLasHorasCambiaCostoAlertaKpiYTableroYConservaLasFechas() throws Exception {
        long asignacionId = crearAsignacion("45");
        Medicion antes = medir(asignacionId);
        assertThat(antes.costoAsignacion).isEqualByComparingTo(COSTO_45);
        assertThat(antes.costoHoja).isEqualByComparingTo(antes.costoAsignacion);
        assertThat(antes.costoProyecto).isEqualByComparingTo(antes.costoHoja);
        assertThat(antes.costoTablero).isEqualByComparingTo(antes.costoProyecto);
        assertThat(antes.costoRealAsignacion).isEqualByComparingTo(CERO);
        assertThat(antes.costoRealHoja).isEqualByComparingTo(CERO);
        assertThat(antes.costoRealProyecto).isEqualByComparingTo(CERO);
        assertThat(antes.costoRealTablero).isEqualByComparingTo(CERO);
        assertThat(antes.alertas).isPositive();
        assertThat(antes.sobreasignados).isPositive();
        assertThat(antes.kp08NoCalculable).isFalse();
        assertThat(antes.kp08).isGreaterThan(CERO);
        assertThat(antes.kp03NoCalculable).isTrue();
        assertThat(antes.inicioPlan).isNotBlank();
        assertThat(antes.finPlan).isNotBlank();

        mvc.perform(put("/api/asignaciones/" + asignacionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("40")))
            .andExpect(status().isOk());

        Medicion despues = medir(asignacionId);
        assertThat(despues.costoAsignacion).isEqualByComparingTo(COSTO_40);
        assertThat(despues.costoAsignacion).isLessThan(antes.costoAsignacion);
        assertThat(despues.costoHoja).isEqualByComparingTo(despues.costoAsignacion);
        assertThat(despues.costoProyecto).isEqualByComparingTo(despues.costoHoja);
        assertThat(despues.costoTablero).isEqualByComparingTo(despues.costoProyecto);
        assertThat(despues.costoRealAsignacion).isEqualByComparingTo(antes.costoRealAsignacion);
        assertThat(despues.costoRealHoja).isEqualByComparingTo(CERO);
        assertThat(despues.costoRealProyecto).isEqualByComparingTo(CERO);
        assertThat(despues.costoRealTablero).isEqualByComparingTo(CERO);
        assertThat(despues.alertas).isZero();
        assertThat(despues.sobreasignados).isZero();
        assertThat(despues.kp08NoCalculable).isFalse();
        assertThat(despues.kp08).isEqualByComparingTo(CERO);
        assertThat(despues.kp08).isLessThan(antes.kp08);
        assertThat(despues.kp03NoCalculable).isTrue();
        assertThat(despues.inicioPlan).isEqualTo(antes.inicioPlan);
        assertThat(despues.finPlan).isEqualTo(antes.finPlan);
        assertThat(despues.fechaFinPlan).isEqualTo(antes.fechaFinPlan);

        String historico08 = cuerpoGet("/api/kpi/" + antes.kp08Id + "/historico?proyectoId=" + proyectoId);
        assertThat(entero(historico08, "$.length()")).isEqualTo(antes.filasKp08 + 1);
        assertThat(monto(historico08, "$[0].valor")).isEqualByComparingTo(despues.kp08);
        assertThat(monto(historico08, "$[1].valor")).isEqualByComparingTo(antes.kp08);
        assertThat(flag(historico08, "$[0].noCalculable")).isFalse();
        assertThat(flag(historico08, "$[1].noCalculable")).isFalse();

        String historico03 = cuerpoGet("/api/kpi/" + antes.kp03Id + "/historico?proyectoId=" + proyectoId);
        assertThat(entero(historico03, "$.length()")).isEqualTo(antes.filasKp03 + 1);
        assertThat(flag(historico03, "$[0].noCalculable")).isTrue();
        assertThat(flag(historico03, "$[1].noCalculable")).isTrue();
    }

    private long crearAsignacion(String cantidad) throws Exception {
        String cuerpo = mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(cantidad)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return Long.parseLong(texto(cuerpo, "$.id"));
    }

    private Medicion medir(long asignacionId) throws Exception {
        String asignacion = cuerpoGet("/api/asignaciones/" + asignacionId);
        String hoja = cuerpoGet("/api/tareas/" + hojaId);
        String proyecto = cuerpoGet("/api/proyectos/" + proyectoId);
        String alertas = cuerpoGet("/api/proyectos/" + proyectoId + "/sobreasignaciones");
        String kpi = cuerpoGet("/api/proyectos/" + proyectoId + "/kpi");
        String tablero = cuerpoGet("/api/tablero/proyecto/" + proyectoId);
        long kp08Id = Long.parseLong(texto(kpi, "$[?(@.codigo=='KP-08')].id"));
        long kp03Id = Long.parseLong(texto(kpi, "$[?(@.codigo=='KP-03')].id"));
        String historico08 = cuerpoGet("/api/kpi/" + kp08Id + "/historico?proyectoId=" + proyectoId);
        String historico03 = cuerpoGet("/api/kpi/" + kp03Id + "/historico?proyectoId=" + proyectoId);
        BigDecimal kp08 = monto(kpi, "$[?(@.codigo=='KP-08')].valor");
        return new Medicion(
            monto(asignacion, "$.costoPlan"),
            monto(asignacion, "$.costoReal"),
            monto(hoja, "$.costoPlan"),
            monto(hoja, "$.costoReal"),
            monto(proyecto, "$.costoPlan"),
            monto(proyecto, "$.costoReal"),
            monto(tablero, "$.costos.plan"),
            monto(tablero, "$.costos.real"),
            entero(alertas, "$.length()"),
            entero(tablero, "$.recursos.sobreasignados"),
            kp08,
            flag(kpi, "$[?(@.codigo=='KP-08')].noCalculable"),
            flag(kpi, "$[?(@.codigo=='KP-03')].noCalculable"),
            texto(hoja, "$.inicioPlan"),
            texto(hoja, "$.finPlan"),
            texto(proyecto, "$.fechaFinPlan"),
            kp08Id,
            kp03Id,
            entero(historico08, "$.length()"),
            entero(historico03, "$.length()"),
            monto(tablero, "$.kpi[?(@.codigo=='KP-08')].valor"),
            flag(tablero, "$.kpi[?(@.codigo=='KP-03')].noCalculable"));
    }

    private String cuerpo(String cantidad) {
        return "{\"tareaId\":" + hojaId + ",\"recursoId\":" + recursoId
            + ",\"cantidad\":" + cantidad + ",\"tarifa\":32000}";
    }

    private String cuerpoGet(String ruta) throws Exception {
        return mvc.perform(get(ruta)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal monto(String json, String path) {
        return new BigDecimal(texto(json, path));
    }

    private static int entero(String json, String path) {
        return Integer.parseInt(texto(json, path));
    }

    private static boolean flag(String json, String path) {
        return Boolean.parseBoolean(texto(json, path));
    }

    private static String texto(String json, String path) {
        Object leido = JsonPath.read(json, path);
        if (leido instanceof List<?> lista) {
            assertThat(lista).as(path).hasSize(1);
            leido = lista.get(0);
        }
        assertThat(leido).as(path).isNotNull();
        return leido.toString();
    }

    private record Medicion(
            BigDecimal costoAsignacion,
            BigDecimal costoRealAsignacion,
            BigDecimal costoHoja,
            BigDecimal costoRealHoja,
            BigDecimal costoProyecto,
            BigDecimal costoRealProyecto,
            BigDecimal costoTablero,
            BigDecimal costoRealTablero,
            int alertas,
            int sobreasignados,
            BigDecimal kp08,
            boolean kp08NoCalculable,
            boolean kp03NoCalculable,
            String inicioPlan,
            String finPlan,
            String fechaFinPlan,
            long kp08Id,
            long kp03Id,
            int filasKp08,
            int filasKp03,
            BigDecimal kp08Tablero,
            boolean kp03TableroNoCalculable) {

        private Medicion {
            assertThat(kp08Tablero).isEqualByComparingTo(kp08);
            assertThat(kp03TableroNoCalculable).isEqualTo(kp03NoCalculable);
        }
    }
}
