package cl.proyecto.gestion.common;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Convierte las excepciones en respuestas { codigo, mensaje, detalles } con el estado HTTP correcto. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> negocio(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorRespuesta(e.getCodigo(), e.getMessage(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorRespuesta("VALIDACION", "Hay datos inválidos en la solicitud.", detalles));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorRespuesta("JSON_INVALIDO", "El cuerpo de la solicitud no es un JSON válido.", List.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorRespuesta("PARAMETRO_INVALIDO", "Valor inválido para el parámetro '" + e.getName() + "'.", List.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorRespuesta("INTEGRIDAD_DE_DATOS", "La operación viola una regla de integridad de los datos.", List.of()));
    }
}
