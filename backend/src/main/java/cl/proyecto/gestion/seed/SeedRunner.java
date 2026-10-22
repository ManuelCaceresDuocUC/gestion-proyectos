package cl.proyecto.gestion.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Carga el caso de prueba (classpath:seed/caso-prueba.json y kpi-definiciones.json) si la base está vacía.
 * TODO: issue C-09 (v1: estructura, recursos), C-13 (v2: dependencias, asignaciones, hitos, cambios).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    @Override
    public void run(String... args) {
        log.info("Seed habilitado, pero la carga del caso de prueba aún no está implementada (issue C-09).");
    }
}
