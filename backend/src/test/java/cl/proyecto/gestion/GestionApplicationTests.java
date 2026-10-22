package cl.proyecto.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Requiere PostgreSQL (en local: docker compose up db; en CI lo levanta el workflow). */
@SpringBootTest
class GestionApplicationTests {
    @Test
    void contextLoads() {}
}
