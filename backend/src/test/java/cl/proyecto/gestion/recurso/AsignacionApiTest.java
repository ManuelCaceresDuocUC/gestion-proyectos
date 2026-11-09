package cl.proyecto.gestion.recurso;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class AsignacionApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;

    Long tareaHojaId;
    Long tareaPadreId;
    Long recursoId;

    @BeforeEach
    void escenario() {
        var proyecto = proyectos.crear(new ProyectoRequest(
            "T-B06-API-" + System.nanoTime(),
            "Proyecto API B-06",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            null,
            LocalDate.of(2026, 11, 5)));

        Tarea etapa = tareas.crear(
            proyecto.id,
            new TareaRequest(null, "Etapa", null, null, null, null));

        Tarea actividad = tareas.crear(
            proyecto.id,
            new TareaRequest(etapa.id, "Actividad", null, null, null, null));

        Tarea subactividad = tareas.crear(
            proyecto.id,
            new TareaRequest(actividad.id, "Subactividad", null, 2, null, null));

        tareaPadreId = actividad.id;
        tareaHojaId = subactividad.id;

        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        recursoId = recursos.crear(new RecursoRequest(
            tipo.id,
            "Ana Pérez-" + System.nanoTime(),
            new BigDecimal("25000.00"),
            new BigDecimal("8.0"),
            true)).id;
    }

    private String cuerpoAsignacion(Long tareaId) {
        return """
            {
              "tareaId": %d,
              "recursoId": %d,
              "cantidad": 1.00,
              "horasSemana": 40.0,
              "semanas": 2.0
            }
            """.formatted(tareaId, recursoId);
    }

    @Test
    void creaAsignacionPorApi() throws Exception {
        mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAsignacion(tareaHojaId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tareaId").value(tareaHojaId))
            .andExpect(jsonPath("$.recursoId").value(recursoId))
            .andExpect(jsonPath("$.cantidad").value(1.0))
            .andExpect(jsonPath("$.tarifa").value(25000.0));
    }

    @Test
    void listaAsignacionesDeUnaTarea() throws Exception {
        mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAsignacion(tareaHojaId)))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/asignaciones/tarea/" + tareaHojaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tareaId").value(tareaHojaId));
    }

    @Test
    void duplicadaDevuelve409() throws Exception {
        mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAsignacion(tareaHojaId)))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAsignacion(tareaHojaId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("ASIGNACION_DUPLICADA"));
    }

    @Test
    void tareaConHijosDevuelve409() throws Exception {
        mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoAsignacion(tareaPadreId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TAREA_NO_HOJA"));
    }

    @Test
    void cantidadInvalidaDevuelve400() throws Exception {
        String cuerpo = """
            {
              "tareaId": %d,
              "recursoId": %d,
              "cantidad": 0,
              "horasSemana": 40.0,
              "semanas": 2.0
            }
            """.formatted(tareaHojaId, recursoId);

        mvc.perform(post("/api/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
            .andExpect(status().isBadRequest());
    }
}