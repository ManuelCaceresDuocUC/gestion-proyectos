package cl.proyecto.gestion.seed;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/** C-07: el runner delega en SeedService. */
class SeedRunnerTest {

    @Test
    void alArrancarLlamaACargar() {
        SeedService seed = mock(SeedService.class);
        when(seed.cargar()).thenReturn(true);
        new SeedRunner(seed).run();
        verify(seed).cargar();
    }

    @Test
    void siYaEstabaTambienLlamaACargar() {
        SeedService seed = mock(SeedService.class);
        when(seed.cargar()).thenReturn(false);
        new SeedRunner(seed).run();
        verify(seed).cargar();
    }
}
