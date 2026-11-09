package cl.proyecto.gestion.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-test", 60);

    @Test
    void emiteYLeeElUsuario() {
        Usuario u = new Usuario();
        u.id = 7L;
        u.email = "admin@demo.cl";
        u.rol = RolUsuario.ADMIN;
        JwtService.JwtUsuario leido = jwt.leer(jwt.emitir(u));
        assertThat(leido.id()).isEqualTo(7L);
        assertThat(leido.email()).isEqualTo("admin@demo.cl");
        assertThat(leido.rol()).isEqualTo(RolUsuario.ADMIN);
    }

    @Test
    void rechazaTokenAlterado() {
        Usuario u = new Usuario();
        u.id = 1L;
        u.email = "a@b.cl";
        u.rol = RolUsuario.USUARIO_PROYECTO;
        String token = jwt.emitir(u);
        assertThatThrownBy(() -> jwt.leer(token + "x")).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaSecretoCorto() {
        assertThatThrownBy(() -> new JwtService("corto", 60))
            .isInstanceOf(IllegalStateException.class);
    }
}
