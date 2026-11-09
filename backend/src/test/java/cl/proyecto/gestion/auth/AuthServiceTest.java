package cl.proyecto.gestion.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired AuthService auth;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;

    @Test
    void loginDevuelveTokenYUsuarioSinHash() {
        Usuario u = guardar("ok-" + System.nanoTime() + "@demo.cl", "clave-ok", RolUsuario.ADMIN, true);
        LoginResponse r = auth.login(u.email.toUpperCase(), "clave-ok");
        assertThat(r.token()).isNotBlank();
        assertThat(r.usuario().id()).isEqualTo(u.id);
        assertThat(r.usuario().rol()).isEqualTo(RolUsuario.ADMIN);
        assertThat(r.usuario().email()).isEqualTo(u.email);
        assertThat(jwt.leer(r.token()).id()).isEqualTo(u.id);
    }

    @Test
    void claveMalaYEmailDesconocidoDanElMismoError() {
        Usuario u = guardar("existe-" + System.nanoTime() + "@demo.cl", "clave-ok", RolUsuario.USUARIO_PROYECTO, true);
        assertCredenciales(() -> auth.login(u.email, "otra"));
        assertCredenciales(() -> auth.login("nadie-" + System.nanoTime() + "@demo.cl", "clave-ok"));
    }

    @Test
    void usuarioInactivoNoEntra() {
        Usuario u = guardar("off-" + System.nanoTime() + "@demo.cl", "clave-ok", RolUsuario.ADMIN, false);
        assertCredenciales(() -> auth.login(u.email, "clave-ok"));
    }

    private Usuario guardar(String email, String clave, RolUsuario rol, boolean activo) {
        Usuario u = new Usuario();
        u.nombre = "Prueba";
        u.email = email;
        u.passwordHash = encoder.encode(clave);
        u.rol = rol;
        u.activo = activo;
        return usuarios.save(u);
    }

    private static void assertCredenciales(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion)
            .isInstanceOf(ApiException.class)
            .satisfies(e -> {
                ApiException api = (ApiException) e;
                assertThat(api.getCodigo()).isEqualTo("CREDENCIALES_INVALIDAS");
                assertThat(api.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(api.getMessage()).isEqualTo("Credenciales inválidas.");
            });
    }
}
