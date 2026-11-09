package cl.proyecto.gestion.auth;

import cl.proyecto.gestion.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String MSG_CREDENCIALES = "Credenciales inválidas.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String hashCiego;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.hashCiego = encoder.encode("x");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String contrasena) {
        String correo = email == null ? "" : email.trim().toLowerCase();
        Usuario u = usuarios.findByEmail(correo).orElse(null);
        String hash = u != null ? u.passwordHash : hashCiego;
        boolean claveOk = encoder.matches(contrasena, hash);
        if (u == null || !u.activo || !claveOk) {
            throw ApiException.noAutenticado("CREDENCIALES_INVALIDAS", MSG_CREDENCIALES);
        }
        return new LoginResponse(jwt.emitir(u), UsuarioResponse.de(u));
    }
}
