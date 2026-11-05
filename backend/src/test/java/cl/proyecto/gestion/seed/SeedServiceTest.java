package cl.proyecto.gestion.seed;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.cambio.SolicitudCambioService;
import cl.proyecto.gestion.planificacion.dependencia.DependenciaService;
import cl.proyecto.gestion.proyecto.EstadoProyecto;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TipoTarea;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecurso;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** C-07: recorte v1 — 4/12/20, recursos, hoja 1.4; no duplica EEC-2026 ni carga C-11. */
@SpringBootTest
@Transactional
class SeedServiceTest {

    @Autowired SeedService seed;
    @Autowired ProyectoService proyectos;
    @Autowired TareaRepository tareas;
    @Autowired TipoRecursoService tipos;
    @Autowired RecursoService recursos;
    @Autowired DependenciaService dependencias;
    @Autowired SolicitudCambioService cambios;

    @Test
    void cargaElRecorteV1YLaSegundaVezNoRepite() {
        assertThat(seed.cargar()).isTrue();
        assertThat(seed.cargar()).isFalse();

        Proyecto p = proyecto();
        assertThat(proyectos.listar(null, null).stream()
            .filter(x -> SeedService.CODIGO.equals(x.codigo))).hasSize(1);
        assertThat(p.nombre).contains("estudio colaborativo");
        assertThat(p.estado).isEqualTo(EstadoProyecto.EN_EJECUCION);
        assertThat(p.fechaInicio).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(p.diasLaborales).isEqualTo("LMXJV");
        assertThat(p.presupuesto).isEqualByComparingTo("21800000");
        assertThat(p.moneda).isEqualTo("CLP");
        assertThat(recursos.obtener(p.responsableId).nombre).isEqualTo("Coordinadora de proyecto");

        assertThat(tipos.listar()).extracting((TipoRecurso t) -> t.nombre)
            .contains("Profesional", "Equipamiento", "Material", "Servicio externo");
        assertThat(recursos.listar(false)).extracting((Recurso r) -> r.nombre)
            .contains(
                "Coordinadora de proyecto", "Ingeniero de infraestructura",
                "Diseñadora de espacios", "Analista de operaciones", "Administrador de compras",
                "Mesas colaborativas", "Sillas ergonómicas",
                "Equipos de red (switch y puntos de acceso)",
                "Paneles de iluminación LED", "Pizarras y señalética",
                "Instalador eléctrico externo", "Licencia del sistema de reservas");

        List<Tarea> edt = tareas.findByProyectoId(p.id);
        assertThat(edt.stream().filter(t -> t.tipo == TipoTarea.ETAPA)).hasSize(4);
        assertThat(edt.stream().filter(t -> t.tipo == TipoTarea.ACTIVIDAD)).hasSize(12);
        assertThat(edt.stream().filter(t -> t.tipo == TipoTarea.SUBACTIVIDAD)).hasSize(20);

        Tarea hoja = porCodigo(edt, "1.4");
        assertThat(hoja.tipo).isEqualTo(TipoTarea.ACTIVIDAD);
        assertThat(hoja.nombre).isEqualTo("Obtener autorización de dirección");
        assertThat(hoja.duracionDias).isEqualTo(5);
        assertThat(hoja.avance).isEqualByComparingTo("100");
        assertThat(edt).noneMatch(t -> hoja.id.equals(t.parentId));
        assertThat(hoja.inicioPlan).isNotNull();
        assertThat(hoja.finPlan).isNotNull();

        Tarea sub = porCodigo(edt, "1.1.1");
        assertThat(sub.tipo).isEqualTo(TipoTarea.SUBACTIVIDAD);
        assertThat(sub.duracionDias).isEqualTo(10);

        assertThat(dependencias.listar(p.id)).isEmpty();
        assertThat(cambios.listar(p.id, null, null, null, null, null, null)).isEmpty();
    }

    @Test
    void noFallaSiLosTiposDelCasoYaExisten() {
        asegurarTipo("Profesional", "hora");
        asegurarTipo("Equipamiento", "unidad");
        asegurarTipo("Material", "unidad");
        asegurarTipo("Servicio externo", "global");
        assertThat(seed.cargar()).isTrue();
        assertThat(tareas.findByProyectoId(proyecto().id)).hasSize(36);
    }

    private Proyecto proyecto() {
        return proyectos.listar(null, null).stream()
            .filter(x -> SeedService.CODIGO.equals(x.codigo))
            .findFirst()
            .orElseThrow();
    }

    private void asegurarTipo(String nombre, String unidad) {
        boolean ya = tipos.listar().stream().anyMatch(t -> t.nombre.equalsIgnoreCase(nombre));
        if (!ya) {
            tipos.crear(new TipoRecursoRequest(nombre, unidad));
        }
    }

    private static Tarea porCodigo(List<Tarea> edt, String codigo) {
        return edt.stream().filter(t -> codigo.equals(t.codigo)).findFirst().orElseThrow();
    }
}
