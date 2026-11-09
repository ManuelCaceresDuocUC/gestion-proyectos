package cl.proyecto.gestion.recurso;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class RecursoApiTest {

    @Autowired MockMvc mvc;
    @Autowired TipoRecursoService tipos;

    @Test
    void creaYConsultaTipoDeRecurso() throws Exception {
        String nombre = "Profesional-" + System.nanoTime();

        mvc.perform(post("/api/tipos-recurso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "%s",
                      "unidad": "hora"
                    }
                    """.formatted(nombre)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value(nombre))
            .andExpect(jsonPath("$.unidad").value("hora"));
    }

    @Test
    void creaYConsultaRecurso() throws Exception {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        mvc.perform(post("/api/recursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoRecursoId": %d,
                      "nombre": "Ana Pérez",
                      "tarifa": 25000.00,
                      "capacidadHorasDia": 8.0,
                      "activo": true
                    }
                    """.formatted(tipo.id)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoRecursoId").value(tipo.id))
            .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
            .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void tipoInexistenteDevuelve400() throws Exception {
        mvc.perform(post("/api/recursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoRecursoId": -1,
                      "nombre": "Ana Pérez",
                      "tarifa": 25000.00,
                      "capacidadHorasDia": 8.0,
                      "activo": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("TIPO_RECURSO_INEXISTENTE"));
    }

    @Test
    void tarifaNegativaDevuelve400() throws Exception {
        TipoRecurso tipo = tipos.crear(
            new TipoRecursoRequest("Profesional-" + System.nanoTime(), "hora"));

        mvc.perform(post("/api/recursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoRecursoId": %d,
                      "nombre": "Ana Pérez",
                      "tarifa": -1,
                      "capacidadHorasDia": 8.0,
                      "activo": true
                    }
                    """.formatted(tipo.id)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listarRecursosSoloActivosPorDefecto() throws Exception {
        mvc.perform(get("/api/recursos"))
            .andExpect(status().isOk());
    }
}