package cl.proyecto.gestion.common;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con un código estable que el frontend puede usar.
 * Se responde como { "codigo": "...", "mensaje": "...", "detalles": [] } (ver ManejadorErrores).
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static ApiException invalido(String codigo, String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }

    public static ApiException conflicto(String codigo, String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
