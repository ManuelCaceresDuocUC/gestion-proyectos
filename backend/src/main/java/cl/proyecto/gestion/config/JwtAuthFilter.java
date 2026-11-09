package cl.proyecto.gestion.config;

import cl.proyecto.gestion.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Si hay Bearer, lo valida y deja el rol en el contexto.
 * Si no hay header, no toca el contexto (así @WithMockUser sigue sirviendo en tests).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final SeguridadJson json;

    public JwtAuthFilter(JwtService jwt, SeguridadJson json) {
        this.jwt = jwt;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.startsWith("Bearer ")) {
            json.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", "Se requiere autenticación.");
            return;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            json.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", "Se requiere autenticación.");
            return;
        }
        try {
            JwtService.JwtUsuario u = jwt.leer(token);
            var auth = new UsernamePasswordAuthenticationToken(
                u.id(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.rol().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            json.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", "Se requiere autenticación.");
            return;
        }
        chain.doFilter(request, response);
    }
}
