package cl.proyecto.gestion.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(max = 100) String contrasena) {}
