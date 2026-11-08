package cl.proyecto.gestion.planificacion.recalculo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecurso;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SobrecargaAlertaApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired AsignacionService asignaciones;

    Long proyectoId;
    Long recursoId;

    @BeforeEach
    void escenario() {
        proyectoId = proyectos.crear(new ProyectoRequest(
            "T-B09-API-" + System.nanoTime(),
            "API sobreasignación",
            null,
            null,
            null,
            BigDecimal.ZERO,
            null,
            "LMXJV",
            LocalDate.of(2026, 11, 9))).id;

        Tarea etapa = tareas.crear(proyectoId,
            new TareaRequest(null, "Etapa", null, null, null, null));
        Long tareaA = tareas.crear(proyectoId,
            new TareaRequest(etapa.id, "Tarea A", null, 2, null, null)).id;
        Long tareaB = tareas.crear(proyectoId,
            new TareaRequest(etapa.id, "Tarea B", null, 2, null, null)).id;

        TipoRecurso tipo = tipos.crear(new TipoRecursoRequest(
            "Profesional-B09-API-" + System.nanoTime(), "hora"));
        recursoId = recursos.crear(new RecursoRequest(
            tipo.id, "Ana Pérez", new BigDecimal("100"), new BigDecimal("8.0"), true)).id;

        asignaciones.crear(new AsignacionRequest(
            tareaA, recursoId, new BigDecimal("16"), null, null, null));
        asignaciones.crear(new AsignacionRequest(
            tareaB, recursoId, new BigDecimal("8"), null, null, null));
    }

    @Test
    void listaAlertasConProfesionalFechaYTareas() throws Exception {
        mvc.perform(get("/api/proyectos/" + proyectoId + "/sobreasignaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].recursoId").value(recursoId.intValue()))
            .andExpect(jsonPath("$[0].profesional").value("Ana Pérez"))
            .andExpect(jsonPath("$[0].fecha").value("2026-11-09"))
            .andExpect(jsonPath("$[0].cargaHoras").value(12.0))
            .andExpect(jsonPath("$[0].capacidadHorasDia").value(8.0))
            .andExpect(jsonPath("$[0].excesoHoras").value(4.0))
            .andExpect(jsonPath("$[0].tareas.length()").value(2))
            .andExpect(jsonPath("$[0].tareas[0].codigo").value("1.1"))
            .andExpect(jsonPath("$[0].tareas[1].codigo").value("1.2"))
            .andExpect(jsonPath("$[1].fecha").value("2026-11-10"));
    }

    @Test
    void proyectoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/proyectos/999999999/sobreasignaciones"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
