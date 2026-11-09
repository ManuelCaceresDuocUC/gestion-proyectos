package cl.proyecto.gestion.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** C-01: health y login públicos; el resto pide JWT. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    private static final String CLAVE = "clave-ok";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    @Test
    void healthSiguePublico() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void sinTokenNoEntraALaApi() throws Exception {
        mvc.perform(get("/api/proyectos"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void bearerInvalidoEs401() throws Exception {
        mvc.perform(get("/api/proyectos").header("Authorization", "Bearer no-es-un-jwt"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void loginDevuelveTokenYEntraALaApi() throws Exception {
        Usuario u = guardar("ok-" + System.nanoTime() + "@demo.cl", RolUsuario.ADMIN, true);
        String token = tokenDe(u.email.toUpperCase(), CLAVE);
        mvc.perform(get("/api/proyectos").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void loginRespondeUsuarioSinHash() throws Exception {
        Usuario u = guardar("json-" + System.nanoTime() + "@demo.cl", RolUsuario.USUARIO_PROYECTO, true);
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(u.email, CLAVE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.usuario.id").value(u.id.intValue()))
            .andExpect(jsonPath("$.usuario.email").value(u.email))
            .andExpect(jsonPath("$.usuario.nombre").value("Prueba"))
            .andExpect(jsonPath("$.usuario.rol").value("USUARIO_PROYECTO"))
            .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }

    @Test
    void claveMalaYEmailDesconocidoDanElMismo401() throws Exception {
        Usuario u = guardar("existe-" + System.nanoTime() + "@demo.cl", RolUsuario.ADMIN, true);
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(u.email, "otra")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
            .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas."));
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("nadie-" + System.nanoTime() + "@demo.cl", CLAVE)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
            .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas."));
    }

    @Test
    void usuarioInactivoNoEntra() throws Exception {
        Usuario u = guardar("off-" + System.nanoTime() + "@demo.cl", RolUsuario.ADMIN, false);
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(u.email, CLAVE)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void loginSinDatosEs400() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"contrasena\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void usuarioProyectoNoApruebaNiConJwt() throws Exception {
        Usuario admin = guardar("adm-" + System.nanoTime() + "@demo.cl", RolUsuario.ADMIN, true);
        Usuario usuario = guardar("usr-" + System.nanoTime() + "@demo.cl", RolUsuario.USUARIO_PROYECTO, true);
        String tokenAdmin = tokenDe(admin.email, CLAVE);
        String tokenUsuario = tokenDe(usuario.email, CLAVE);

        String proyecto = mvc.perform(post("/api/proyectos")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"T-AUTH-" + (System.nanoTime() % 1_000_000) + "\",\"nombre\":\"Auth\",\"presupuesto\":1}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String proyectoId = idDe(proyecto);

        String creada = mvc.perform(post("/api/proyectos/" + proyectoId + "/cambios")
                .header("Authorization", "Bearer " + tokenUsuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Sin permiso\",\"tipo\":\"TIEMPO\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String cambioId = idDe(creada);

        mvc.perform(put("/api/cambios/" + cambioId)
                .header("Authorization", "Bearer " + tokenUsuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Sin permiso\",\"tipo\":\"TIEMPO\",\"estado\":\"EN_EVALUACION\"}"))
            .andExpect(status().isOk());

        mvc.perform(post("/api/cambios/" + cambioId + "/aprobar")
                .header("Authorization", "Bearer " + tokenUsuario))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));

        mvc.perform(put("/api/cambios/" + cambioId)
                .header("Authorization", "Bearer " + tokenUsuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Sin permiso\",\"tipo\":\"TIEMPO\",\"estado\":\"APROBADO\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    private Usuario guardar(String email, RolUsuario rol, boolean activo) {
        Usuario u = new Usuario();
        u.nombre = "Prueba";
        u.email = email;
        u.passwordHash = encoder.encode(CLAVE);
        u.rol = rol;
        u.activo = activo;
        return usuarios.save(u);
    }

    private String tokenDe(String email, String clave) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, clave)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String loginJson(String email, String clave) {
        return "{\"email\":\"" + email + "\",\"contrasena\":\"" + clave + "\"}";
    }

    private static String idDe(String json) {
        return json.replaceAll(".*\"id\":(\\d+).*", "$1");
    }
}
