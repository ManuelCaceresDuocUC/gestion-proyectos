package cl.proyecto.gestion.auth;

public record LoginResponse(String token, UsuarioResponse usuario) {}
