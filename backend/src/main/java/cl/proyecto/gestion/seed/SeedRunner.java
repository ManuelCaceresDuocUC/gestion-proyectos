package cl.proyecto.gestion.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, carga el caso de prueba (usuarios, v1 y v2) si {@code APP_SEED_ENABLED=true}.
 * No duplica: cada capa (usuarios, proyecto, dependencias, línea base, solicitudes, hitos) se salta si ya está.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final SeedService seed;

    public SeedRunner(SeedService seed) {
        this.seed = seed;
    }

    @Override
    public void run(String... args) {
        if (seed.cargar()) {
            log.info("Seed C-07: caso de prueba cargado.");
        } else {
            log.info("Seed C-07: el caso ya estaba, no se repite.");
        }
    }
}
