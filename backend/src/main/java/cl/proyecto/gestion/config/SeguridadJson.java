package cl.proyecto.gestion.config;

import cl.proyecto.gestion.common.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Escribe 401/403 con el mismo JSON que el resto de la API. No incluye tokens ni secretos. */
@Component
public class SeguridadJson {

    private final ObjectMapper mapper;

    public SeguridadJson(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escribir(HttpServletResponse response, int status, String codigo, String mensaje) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorRespuesta(codigo, mensaje, List.of()));
    }
}
