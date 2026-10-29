package cl.proyecto.gestion.common;

import java.util.List;

/** Cuerpo uniforme de los errores de la API. "detalles" lista los campos inválidos (puede venir vacío). */
public record ErrorRespuesta(String codigo, String mensaje, List<String> detalles) {}
