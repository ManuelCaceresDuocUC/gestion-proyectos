package cl.proyecto.gestion.cambio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.auth.RolUsuario;
import cl.proyecto.gestion.auth.Usuario;
import cl.proyecto.gestion.auth.UsuarioRepository;
import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** C-05: evidencias y usuarioId del historial. */
@SpringBootTest
@Transactional
class EvidenciaServiceTest {

    @Autowired EvidenciaService evidencias;
    @Autowired SolicitudCambioService solicitudes;
    @Autowired ProyectoService proyectos;
    @Autowired CambioHistorialRepository historial;
    @Autowired UsuarioRepository usuarios;

    Long proyectoId;
    Long usuarioId;

    @BeforeEach
    void escenario() {
        Usuario u = new Usuario();
        u.nombre = "C-05";
        u.email = "c05-" + System.nanoTime() + "@demo.cl";
        u.passwordHash = "x";
        u.rol = RolUsuario.ADMIN;
        u.activo = true;
        usuarios.save(u);
        usuarioId = u.id;
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                usuarioId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        proyectoId = proyectos.crear(new ProyectoRequest(
            "T-C05-" + System.nanoTime() % 1_000_000,
            "Evidencias",
            null, null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void evidenciaGuardaQuienLaCreo() {
        SolicitudCambio s = solicitudes.crear(proyectoId, req("Con acta"));
        Evidencia e = evidencias.crear(s.id, new EvidenciaRequest("Acta", "https://ejemplo.cl/a"));
        assertThat(e.id).isNotNull();
        assertThat(e.descripcion).isEqualTo("Acta");
        assertThat(e.url).isEqualTo("https://ejemplo.cl/a");
        assertThat(e.creadoPor).isEqualTo(usuarioId);
        assertThat(evidencias.listar(s.id)).hasSize(1);
    }

    @Test
    void historialGuardaUsuarioDelJwt() {
        SolicitudCambio s = solicitudes.crear(proyectoId, req("Flujo"));
        solicitudes.actualizar(s.id, req("Flujo", EstadoSolicitud.EN_EVALUACION));
        List<CambioHistorial> filas = historial.findBySolicitudIdOrderByFechaAsc(s.id);
        assertThat(filas).hasSize(1);
        CambioHistorial fila = filas.getFirst();
        assertThat(fila.campo).isEqualTo("estado");
        assertThat(fila.usuarioId).isEqualTo(this.usuarioId);
        assertThat(fila.comentario).isNull();
    }

    @Test
    void eliminarEvidenciaDeOtraSolicitudEs404() {
        SolicitudCambio a = solicitudes.crear(proyectoId, req("A"));
        SolicitudCambio b = solicitudes.crear(proyectoId, req("B"));
        Evidencia e = evidencias.crear(a.id, new EvidenciaRequest("Solo A", null));
        assertThatThrownBy(() -> evidencias.eliminar(b.id, e.id))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("NO_ENCONTRADO");
        assertThat(evidencias.listar(a.id)).hasSize(1);
    }

    @Test
    void noCreaEnProyectoCerrado() {
        SolicitudCambio s = solicitudes.crear(proyectoId, req("Tarde"));
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> evidencias.crear(s.id, new EvidenciaRequest("Tarde", null)))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
    }

    private static SolicitudCambioRequest req(String nombre) {
        return req(nombre, null);
    }

    private static SolicitudCambioRequest req(String nombre, EstadoSolicitud estado) {
        return new SolicitudCambioRequest(
            nombre, null, null, null, null, null, null, null, null,
            TipoSolicitud.ALCANCE, null, null, estado, null, null, null, null, null, null, null, null, null, null);
    }
}
