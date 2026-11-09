package cl.proyecto.gestion.config;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * C-01: JWT. ADMIN aprueba cambios y (cuando existan) administra usuarios y KPI.
 * USUARIO_PROYECTO lee y trabaja el plan, recursos y solicitudes; no aprueba ni rechaza.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFiltro, SeguridadJson json) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> json.escribir(
                    res, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", "Se requiere autenticación."))
                .accessDeniedHandler((req, res, ex) -> json.escribir(
                    res, HttpServletResponse.SC_FORBIDDEN, "PROHIBIDO", "No tiene permiso para esta operación.")))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cambios/*/aprobar", "/api/cambios/*/rechazar").hasRole("ADMIN")
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/kpi/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/kpi/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/kpi/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/kpi/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Evita que Spring genere un usuario in-memory. El login va por AuthService. */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    /** El filtro solo vive en la cadena de Spring Security, no como filtro del contenedor. */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtFiltroRegistro(JwtAuthFilter filtro) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filtro);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") List<String> origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
