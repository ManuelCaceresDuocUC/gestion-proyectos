package cl.proyecto.gestion.planificacion.motor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MotorPlanificacionTest {
    private final MotorPlanificacion motor = new MotorPlanificacion();

    @Test
    void aceptaDependenciasSinCiclo() {
        var deps = List.of(new DependenciaPlan(1, 2, TipoDependencia.FS, 0), new DependenciaPlan(2, 3, TipoDependencia.SS, 0));
        assertDoesNotThrow(() -> motor.validarSinCiclos(List.of(1L, 2L, 3L), deps));
    }

    @Test
    void rechazaCiclo() {
        var deps = List.of(new DependenciaPlan(1, 2, TipoDependencia.FS, 0), new DependenciaPlan(2, 1, TipoDependencia.FF, 0));
        assertThrows(CicloDetectadoException.class, () -> motor.validarSinCiclos(List.of(1L, 2L), deps));
    }

    @Disabled("A-07: FS con desfase")
    @Test
    void fsConDesfase() {}

    @Disabled("A-07: SS")
    @Test
    void ss() {}

    @Disabled("A-08: FF")
    @Test
    void ff() {}

    @Disabled("A-08: SF")
    @Test
    void sf() {}
}
