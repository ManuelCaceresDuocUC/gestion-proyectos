package cl.proyecto.gestion.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Emite y lee JWT HS256. No registra el secreto ni el token. */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiraMinutos;

    public JwtService(
        @Value("${app.jwt.secret}") String secreto,
        @Value("${app.jwt.expira-minutos}") long expiraMinutos) {
        byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiraMinutos = expiraMinutos;
    }

    public String emitir(Usuario u) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(expiraMinutos * 60);
        return Jwts.builder()
            .subject(String.valueOf(u.id))
            .claim("email", u.email)
            .claim("rol", u.rol.name())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(expira))
            .signWith(clave)
            .compact();
    }

    public JwtUsuario leer(String token) {
        Claims c = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        return new JwtUsuario(
            Long.parseLong(c.getSubject()),
            c.get("email", String.class),
            RolUsuario.valueOf(c.get("rol", String.class)));
    }

    public record JwtUsuario(Long id, String email, RolUsuario rol) {}
}
