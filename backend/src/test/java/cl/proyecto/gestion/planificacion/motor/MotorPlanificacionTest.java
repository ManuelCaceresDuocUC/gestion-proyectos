package cl.proyecto.gestion.planificacion.motor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A-07/A-08/A-09. Dominio puro: no necesita base de datos. 2026-11-02 es lunes. */
class MotorPlanificacionTest {
    private final MotorPlanificacion motor = new MotorPlanificacion();
    private final Calendario cal = new Calendario(Set.of());

    static final LocalDate LUN2 = LocalDate.of(2026, 11, 2);
    static LocalDate d(int dia) { return LocalDate.of(2026, 11, dia); }

    static TareaPlan t(long id, int dur) { return new TareaPlan(id, dur, null); }
    static DependenciaPlan dep(long a, long b, TipoDependencia tipo, int lag) {
        return new DependenciaPlan(a, b, tipo, lag);
    }

    Map<Long, FechasCalculadas> calc(List<TareaPlan> ts, List<DependenciaPlan> ds) {
        return motor.calcular(LUN2, ts, ds, cal);
    }

    @Test
    void aceptaDependenciasSinCiclo() {
        var deps = List.of(dep(1, 2, TipoDependencia.FS, 0), dep(2, 3, TipoDependencia.SS, 0));
        assertDoesNotThrow(() -> motor.validarSinCiclos(List.of(1L, 2L, 3L), deps));
    }

    @Test
    void rechazaCiclo() {
        var deps = List.of(dep(1, 2, TipoDependencia.FS, 0), dep(2, 1, TipoDependencia.FF, 0));
        assertThrows(CicloDetectadoException.class, () -> motor.validarSinCiclos(List.of(1L, 2L), deps));
        assertThrows(CicloDetectadoException.class, () -> calc(List.of(t(1, 1), t(2, 1)), deps));
    }

    @Test
    void dependenciaConTareaDesconocidaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> calc(List.of(t(1, 1)), List.of(dep(1, 9, TipoDependencia.FS, 0))));
    }

    @Test
    void sinPredecesorasEmpiezaEnElInicioDelProyectoYSaltaFinDeSemana() {
        var r = calc(List.of(t(1, 3), t(2, 5)), List.of());
        assertEquals(new FechasCalculadas(LUN2, d(4)), r.get(1L));
        assertEquals(new FechasCalculadas(LUN2, d(6)), r.get(2L));
    }

    @Test
    void inicioDeProyectoEnSabadoPasaAlLunes() {
        var r = motor.calcular(d(7), List.of(t(1, 2)), List.of(), cal);
        assertEquals(new FechasCalculadas(d(9), d(10)), r.get(1L));
    }

    @Test
    void feriadoNoCuentaComoDiaHabil() {
        var conFeriado = new Calendario(Set.of(d(3)));
        var r = motor.calcular(LUN2, List.of(t(1, 3)), List.of(), conFeriado);
        assertEquals(new FechasCalculadas(LUN2, d(5)), r.get(1L)); // lun, mie, jue
    }

    @Test
    void duracionCeroEsHito() {
        var r = calc(List.of(t(1, 3), t(2, 0)), List.of(dep(1, 2, TipoDependencia.FS, 0)));
        assertEquals(new FechasCalculadas(d(5), d(5)), r.get(2L));
    }

    @Test
    void fsEmpiezaElDiaHabilSiguiente() {
        var r = calc(List.of(t(1, 3), t(2, 2)), List.of(dep(1, 2, TipoDependencia.FS, 0)));
        assertEquals(new FechasCalculadas(d(5), d(6)), r.get(2L));
    }

    @Test
    void fsDespuesDeViernesPasaAlLunes() {
        var r = calc(List.of(t(1, 5), t(2, 1)), List.of(dep(1, 2, TipoDependencia.FS, 0)));
        assertEquals(new FechasCalculadas(d(9), d(9)), r.get(2L));
    }

    @Test
    void fsConDesfasePositivoYNegativo() {
        var atraso = calc(List.of(t(1, 3), t(2, 1)), List.of(dep(1, 2, TipoDependencia.FS, 2)));
        assertEquals(d(9), atraso.get(2L).inicio()); // jue, vie, (lun) => 2 dias habiles de espera
        var adelanto = calc(List.of(t(1, 3), t(2, 1)), List.of(dep(1, 2, TipoDependencia.FS, -1)));
        assertEquals(d(4), adelanto.get(2L).inicio()); // se solapa con el ultimo dia de la predecesora
    }

    @Test
    void ssEmpiezaJuntaConDesfase() {
        var r = calc(List.of(t(1, 3), t(2, 2)), List.of(dep(1, 2, TipoDependencia.SS, 1)));
        assertEquals(new FechasCalculadas(d(3), d(4)), r.get(2L));
    }

    @Test
    void ffTerminanJuntas() {
        var r = calc(List.of(t(1, 5), t(2, 2)), List.of(dep(1, 2, TipoDependencia.FF, 0)));
        assertEquals(new FechasCalculadas(d(5), d(6)), r.get(2L));
    }

    @Test
    void ffConDesfase() {
        var r = calc(List.of(t(1, 3), t(2, 2)), List.of(dep(1, 2, TipoDependencia.FF, 1)));
        assertEquals(new FechasCalculadas(d(4), d(5)), r.get(2L)); // termina 1 dia habil despues de la predecesora
    }

    @Test
    void sfLaSucesoraTerminaCuandoEmpiezaLaPredecesora() {
        var ts = List.of(new TareaPlan(1, 2, d(9)), t(2, 3));
        var r = calc(ts, List.of(dep(1, 2, TipoDependencia.SF, 0)));
        assertEquals(new FechasCalculadas(d(9), d(10)), r.get(1L));
        assertEquals(new FechasCalculadas(d(5), d(9)), r.get(2L)); // jue, vie, lun: termina el lunes que empieza la 1
    }

    @Test
    void sfNuncaEmpiezaAntesDelInicioDelProyecto() {
        var r = calc(List.of(t(1, 2), t(2, 3)), List.of(dep(1, 2, TipoDependencia.SF, 0)));
        assertEquals(new FechasCalculadas(LUN2, d(4)), r.get(2L));
    }

    @Test
    void conVariasPredecesorasGanaLaMasRestrictiva() {
        var ts = List.of(t(1, 3), t(2, 5), t(3, 1));
        var ds = List.of(dep(1, 3, TipoDependencia.FS, 0), dep(2, 3, TipoDependencia.FS, 0));
        assertEquals(d(9), calc(ts, ds).get(3L).inicio());
    }

    @Test
    void inicioMinimoDeLaTareaSeRespeta() {
        var r = calc(List.of(new TareaPlan(1, 2, d(11))), List.of());
        assertEquals(new FechasCalculadas(d(11), d(12)), r.get(1L));
    }

    @Test
    void cadenaConLosCuatroTipos() {
        // 1 (3d) -FS-> 2 (2d) -SS+1-> 3 (4d) -FF-> 4 (2d)
        var ts = List.of(t(1, 3), t(2, 2), t(3, 4), t(4, 2));
        var ds = List.of(dep(1, 2, TipoDependencia.FS, 0), dep(2, 3, TipoDependencia.SS, 1),
            dep(3, 4, TipoDependencia.FF, 0));
        var r = calc(ts, ds);
        assertEquals(new FechasCalculadas(d(5), d(6)), r.get(2L));
        assertEquals(new FechasCalculadas(d(6), d(11)), r.get(3L));
        assertEquals(new FechasCalculadas(d(10), d(11)), r.get(4L));
    }

    @Test
    void cambiarUnaDuracionMueveALasSucesoras() {
        var ds = List.of(dep(1, 2, TipoDependencia.FS, 0));
        assertEquals(d(5), calc(List.of(t(1, 3), t(2, 1)), ds).get(2L).inicio());
        assertEquals(d(10), calc(List.of(t(1, 6), t(2, 1)), ds).get(2L).inicio());
    }
}
