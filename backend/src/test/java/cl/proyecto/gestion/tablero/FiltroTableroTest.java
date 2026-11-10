package cl.proyecto.gestion.tablero;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.cambio.EstadoSolicitud;
import cl.proyecto.gestion.cambio.PrioridadSolicitud;
import cl.proyecto.gestion.cambio.RiesgoSolicitud;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.hito.Hito;
import cl.proyecto.gestion.proyecto.Tarea;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FiltroTableroTest {

    @Test
    void solicitudesNulasNoRecortan() {
        SolicitudCambio a = solicitud(1L, EstadoSolicitud.ABIERTO, PrioridadSolicitud.ALTA, RiesgoSolicitud.CRITICO, 9L);
        SolicitudCambio b = solicitud(2L, EstadoSolicitud.CERRADO, PrioridadSolicitud.BAJA, RiesgoSolicitud.BAJO, 8L);

        assertThat(FiltroTablero.solicitudes(List.of(a, b), null, null, null, null)).containsExactly(a, b);
    }

    @Test
    void solicitudesFiltraPorEstadoPrioridadRiesgoYResponsable() {
        SolicitudCambio a = solicitud(1L, EstadoSolicitud.ABIERTO, PrioridadSolicitud.ALTA, RiesgoSolicitud.CRITICO, 9L);
        SolicitudCambio b = solicitud(2L, EstadoSolicitud.CERRADO, PrioridadSolicitud.BAJA, RiesgoSolicitud.BAJO, 8L);

        assertThat(FiltroTablero.solicitudes(List.of(a, b), EstadoSolicitud.ABIERTO, null, null, null)).containsExactly(a);
        assertThat(FiltroTablero.solicitudes(List.of(a, b), null, PrioridadSolicitud.BAJA, null, null)).containsExactly(b);
        assertThat(FiltroTablero.solicitudes(List.of(a, b), null, null, RiesgoSolicitud.CRITICO, null)).containsExactly(a);
        assertThat(FiltroTablero.solicitudes(List.of(a, b), null, null, null, 8L)).containsExactly(b);
        assertThat(FiltroTablero.solicitudes(
                List.of(a, b), EstadoSolicitud.ABIERTO, PrioridadSolicitud.BAJA, null, null)).isEmpty();
    }

    @Test
    void ramaIncluyeLaRaizYSusDescendientes() {
        Tarea etapa = tarea(10L, null);
        Tarea actividad = tarea(11L, 10L);
        Tarea hoja = tarea(12L, 11L);
        Tarea otra = tarea(20L, null);

        assertThat(FiltroTablero.rama(10L, List.of(etapa, actividad, hoja, otra)))
                .containsExactlyInAnyOrder(10L, 11L, 12L);
        assertThat(FiltroTablero.rama(99L, List.of(etapa))).isEmpty();
    }

    @Test
    void fechaNulaSoloPasaSiNoHayRango() {
        LocalDate dia = LocalDate.of(2026, 6, 1);
        assertThat(FiltroTablero.fechaEnRango(null, null, null)).isTrue();
        assertThat(FiltroTablero.fechaEnRango(null, dia, null)).isFalse();
        assertThat(FiltroTablero.fechaEnRango(dia, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(FiltroTablero.fechaEnRango(dia, LocalDate.of(2026, 7, 1), null)).isFalse();
    }

    @Test
    void hitoSinVinculoQuedaFueraDeLaRama() {
        Hito hito = new Hito();
        hito.id = 1L;
        hito.fechaPlan = LocalDate.of(2026, 6, 1);
        hito.responsableId = 3L;

        assertThat(FiltroTablero.hitos(
                List.of(hito), Set.of(10L), null, null, null, Map.of())).isEmpty();
        assertThat(FiltroTablero.hitos(
                List.of(hito), Set.of(10L), null, null, null, Map.of(1L, List.of(10L)))).containsExactly(hito);
        assertThat(FiltroTablero.hitos(
                List.of(hito), null, null, null, 9L, Map.of())).isEmpty();
    }

    private static SolicitudCambio solicitud(
            Long id, EstadoSolicitud estado, PrioridadSolicitud prioridad, RiesgoSolicitud riesgo, Long responsableId) {
        SolicitudCambio s = new SolicitudCambio();
        s.id = id;
        s.estado = estado;
        s.prioridad = prioridad;
        s.riesgo = riesgo;
        s.responsableId = responsableId;
        return s;
    }

    private static Tarea tarea(Long id, Long parentId) {
        Tarea t = new Tarea();
        t.id = id;
        t.parentId = parentId;
        return t;
    }
}
