package cl.proyecto.gestion.planificacion.calendario;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.proyecto.EstadoProyecto;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class NoLaborableServiceTest {

    @Mock NoLaborableRepository repo;
    @Mock ProyectoService proyectos;
    @Mock RecalculoService recalculo;

    NoLaborableService servicio;

    @BeforeEach
    void setUp() {
        servicio = new NoLaborableService(repo, proyectos, recalculo);
    }

    @Test
    void recalculaProyectoAlCrearActualizarYEliminarFeriado() {
        Long proyectoId = 7L;
        when(repo.saveAndFlush(any(NoLaborable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NoLaborable existente = feriado(proyectoId, LocalDate.of(2026, 11, 3));
        existente.id = 10L;
        when(repo.findById(10L)).thenReturn(java.util.Optional.of(existente));

        servicio.crear(new NoLaborableRequest(proyectoId, LocalDate.of(2026, 11, 4), "Nuevo"));
        servicio.actualizar(10L, new NoLaborableRequest(proyectoId, LocalDate.of(2026, 11, 5), "Actualizado"));
        servicio.eliminar(10L);

        verify(recalculo, org.mockito.Mockito.times(3)).recalcular(proyectoId);
        verify(repo).flush();
    }

    @Test
    void recalculaTodosLosProyectosEditablesCuandoCambiaAmbitoGlobal() {
        Proyecto editable = proyecto(1L, EstadoProyecto.PLANIFICADO);
        Proyecto terminado = proyecto(2L, EstadoProyecto.TERMINADO);
        when(repo.saveAndFlush(any(NoLaborable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(proyectos.listar(null, null)).thenReturn(List.of(editable, terminado));

        servicio.crear(new NoLaborableRequest(null, LocalDate.of(2026, 11, 4), "General"));

        verify(recalculo).recalcular(1L);
        verify(recalculo, never()).recalcular(2L);
    }

    @Test
    void noPermiteActualizarFeriadoDeProyectoCerrado() {
        Long proyectoId = 7L;
        NoLaborable existente = feriado(proyectoId, LocalDate.of(2026, 11, 3));
        existente.id = 10L;
        when(repo.findById(10L)).thenReturn(java.util.Optional.of(existente));
        when(proyectos.obtenerEditable(proyectoId))
            .thenThrow(ApiException.conflicto("PROYECTO_CERRADO", "Proyecto cerrado"));

        assertThrows(ApiException.class, () ->
            servicio.actualizar(10L, new NoLaborableRequest(null, LocalDate.of(2026, 11, 5), "General")));
        verify(repo, never()).saveAndFlush(any(NoLaborable.class));
    }

    private NoLaborable feriado(Long proyectoId, LocalDate fecha) {
        NoLaborable noLaborable = new NoLaborable();
        noLaborable.proyectoId = proyectoId;
        noLaborable.fecha = fecha;
        return noLaborable;
    }

    private Proyecto proyecto(Long id, EstadoProyecto estado) {
        Proyecto proyecto = new Proyecto();
        proyecto.id = id;
        proyecto.estado = estado;
        return proyecto;
    }
}
