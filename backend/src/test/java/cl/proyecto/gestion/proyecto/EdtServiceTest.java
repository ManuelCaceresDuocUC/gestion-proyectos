package cl.proyecto.gestion.proyecto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.proyecto.gestion.common.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** A-05. Usa la base real; cada test se revierte al terminar. */
@SpringBootTest
@Transactional
class EdtServiceTest {

    @Autowired ProyectoService proyectos;
    @Autowired TareaService tareas;

    Long proyectoId;

    @BeforeEach
    void crearProyecto() {
        proyectoId = proyectos.crear(new ProyectoRequest("T-A05-" + System.nanoTime() % 1_000_000, "EDT", null,
            null, null, BigDecimal.ZERO, null, null, null)).id;
    }

    Tarea nueva(Long parentId, String nombre) {
        return tareas.crear(proyectoId, new TareaRequest(parentId, nombre, null, null, null, null));
    }

    @Test
    void tipoYCodigoSeDeducenDelPadre() {
        Tarea e1 = nueva(null, "Etapa 1");
        Tarea e2 = nueva(null, "Etapa 2");
        Tarea a = nueva(e1.id, "Actividad");
        Tarea s = nueva(a.id, "Sub");
        assertThat(e1.tipo).isEqualTo(TipoTarea.ETAPA);
        assertThat(e1.codigo).isEqualTo("1");
        assertThat(e2.codigo).isEqualTo("2");
        assertThat(a.tipo).isEqualTo(TipoTarea.ACTIVIDAD);
        assertThat(a.codigo).isEqualTo("1.1");
        assertThat(s.tipo).isEqualTo(TipoTarea.SUBACTIVIDAD);
        assertThat(s.codigo).isEqualTo("1.1.1");
        assertThat(s.duracionDias).isEqualTo(1);
    }

    @Test
    void subactividadNoTieneHijos() {
        Tarea s = nueva(nueva(nueva(null, "E").id, "A").id, "S");
        assertThatThrownBy(() -> nueva(s.id, "X"))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("JERARQUIA_INVALIDA");
    }

    @Test
    void moverRenumeraHermanosYDescendientes() {
        Tarea e1 = nueva(null, "E1");
        Tarea e2 = nueva(null, "E2");
        Tarea a = nueva(e1.id, "A de E1");
        tareas.mover(e2.id, 1);
        assertThat(tareas.obtener(e2.id).codigo).isEqualTo("1");
        assertThat(tareas.obtener(e1.id).codigo).isEqualTo("2");
        assertThat(tareas.obtener(a.id).codigo).isEqualTo("2.1");
    }

    @Test
    void posicionFueraDeRangoEsInvalida() {
        Tarea e1 = nueva(null, "E1");
        assertThatThrownBy(() -> tareas.mover(e1.id, 5))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("POSICION_INVALIDA");
    }

    @Test
    void eliminarRenumeraYBorraDescendientes() {
        Tarea e1 = nueva(null, "E1");
        Tarea e2 = nueva(null, "E2");
        Tarea a2 = nueva(e2.id, "A de E2");
        tareas.eliminar(e1.id);
        assertThat(tareas.obtener(e2.id).codigo).isEqualTo("1");
        assertThat(tareas.obtener(a2.id).codigo).isEqualTo("1.1");
        assertThat(tareas.arbol(proyectoId)).hasSize(1);
    }

    @Test
    void arbolAnidaHijos() {
        Tarea e = nueva(null, "E");
        nueva(e.id, "A1");
        nueva(e.id, "A2");
        List<NodoEdt> arbol = tareas.arbol(proyectoId);
        assertThat(arbol).hasSize(1);
        assertThat(arbol.get(0).hijos()).extracting(NodoEdt::codigo).containsExactly("1.1", "1.2");
    }

    @Test
    void padreDeOtroProyectoSeRechaza() {
        Long otro = proyectos.crear(new ProyectoRequest("T-A05-O" + System.nanoTime() % 1_000_000, "Otro", null,
            null, null, BigDecimal.ZERO, null, null, null)).id;
        Tarea ajena = tareas.crear(otro, new TareaRequest(null, "Ajena", null, null, null, null));
        assertThatThrownBy(() -> nueva(ajena.id, "X"))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PADRE_DE_OTRO_PROYECTO");
    }

    @Test
    void proyectoCerradoNoSeModifica() {
        Tarea e = nueva(null, "E");
        proyectos.cerrar(proyectoId);
        assertThatThrownBy(() -> nueva(null, "Otra"))
            .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PROYECTO_CERRADO");
        assertThatThrownBy(() -> tareas.eliminar(e.id)).isInstanceOf(ApiException.class);
    }
}
