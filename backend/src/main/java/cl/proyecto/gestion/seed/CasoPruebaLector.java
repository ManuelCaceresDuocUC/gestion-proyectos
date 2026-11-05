package cl.proyecto.gestion.seed;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Lee {@code classpath:seed/caso-prueba.json} (Maven copia {@code db/seed}).
 * Semanas del JSON → días hábiles: {@code semanas × 5} (calendario LMXJV).
 */
public final class CasoPruebaLector {

    static final String RECURSO = "seed/caso-prueba.json";
    static final int DIAS_POR_SEMANA = 5;

    private static final ObjectMapper JSON = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private CasoPruebaLector() {}

    public static CasoPrueba leer() {
        try (InputStream in = CasoPruebaLector.class.getClassLoader().getResourceAsStream(RECURSO)) {
            if (in == null) {
                throw new IllegalStateException("No está empaquetado " + RECURSO + ".");
            }
            return JSON.readValue(in, CasoPrueba.class);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RECURSO, e);
        }
    }

    /** {@code null} o 0 semanas → 0 días. Una semana laboral = 5 días. */
    public static int diasDe(Integer semanas) {
        if (semanas == null) {
            return 0;
        }
        return semanas * DIAS_POR_SEMANA;
    }
}
