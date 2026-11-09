package cl.proyecto.gestion.hito;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.seed.SeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** B-11: listado de H1–H6 y alerta si la semana cae antes del término. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class HitoApiTest {

    @Autowired MockMvc mvc;
    @Autowired SeedService seed;
    @Autowired ProyectoService proyectos;
    @Autowired HitoService hitos;

    @Test
    void listaLosSeisYRechazaUnaSemanaAdelantada() throws Exception {
        seed.cargar();
        Proyecto p = proyectos.listar(null, null).stream()
            .filter(x -> "EEC-2026".equals(x.codigo))
            .findFirst()
            .orElseThrow();
        HitoResponse h1 = hitos.listar(p.id).stream()
            .filter(h -> "H1".equals(h.codigo()))
            .findFirst()
            .orElseThrow();

        mvc.perform(get("/api/proyectos/" + p.id + "/hitos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(6))
            .andExpect(jsonPath("$[?(@.codigo == 'H1')].estado").value(hasItem("PENDIENTE")))
            .andExpect(jsonPath("$[?(@.codigo == 'H1')].semanaPrevista").value(hasItem(3)));

        String cuerpo = """
            {
              "codigo": "H1",
              "logro": "%s",
              "semanaPrevista": 1,
              "evidencia": "%s",
              "indicador": "%s",
              "criterioAceptacion": "%s",
              "validador": "%s",
              "accionSiNoCumple": "%s",
              "estado": "PENDIENTE",
              "tareaIds": [%d]
            }
            """.formatted(
            escapar(h1.logro()), escapar(h1.evidencia()), escapar(h1.indicador()),
            escapar(h1.criterioAceptacion()), escapar(h1.validador()), escapar(h1.accionSiNoCumple()),
            h1.tareaIds().getFirst());

        mvc.perform(put("/api/hitos/" + h1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("HITO_ANTES_DE_ACTIVIDADES"))
            .andExpect(jsonPath("$.mensaje").value(containsString("anterior al término")));
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
