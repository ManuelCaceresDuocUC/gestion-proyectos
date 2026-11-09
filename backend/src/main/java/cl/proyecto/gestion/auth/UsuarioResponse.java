package cl.proyecto.gestion.auth;

/** Datos públicos del usuario. Sin hash ni contraseña. */
public record UsuarioResponse(Long id, String email, String nombre, RolUsuario rol) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.id, u.email, u.nombre, u.rol);
    }
}
