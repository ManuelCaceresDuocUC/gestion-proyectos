package cl.proyecto.gestion.hito;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.cambio.SolicitudCambioRepository;
import cl.proyecto.gestion.planificacion.calendario.CalendarioProyectos;
import cl.proyecto.gestion.planificacion.cronograma.CronogramaService;
import cl.proyecto.gestion.planificacion.cronograma.TareaPatch;
import cl.proyecto.gestion.planificacion.motor.Calendario;
import cl.proyecto.gestion.planificacion.recalculo.OrdenRecalculo;
import cl.proyecto.gestion.planificacion.recalculo.PasoRecalculo;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlerta;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaRepository;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaTareaRepository;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import cl.proyecto.gestion.recurso.AsignacionRequest;
import cl.proyecto.gestion.recurso.AsignacionService;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.seed.SeedService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * B-16 sobre el seed EEC-2026. Si ese proyecto ya está en PostgreSQL, se borra dentro de
 * esta transacción y {@code seed.cargar()} tiene que volver a crearlo. El rollback no toca la base.
 */
@SpringBootTest
@Transactional
class PasoHitosIntegracionTest {

    private static final String CODIGO_SEED = "EEC-2026";
    private static final BigDecimal COSTO_45H = new BigDecimal("1440000.00");
    private static final BigDecimal COSTO_40H = new BigDecimal("1280000.00");

    @Autowired List<PasoRecalculo> pasos;
    @Autowired SeedService seed;
    @Autowired ProyectoRepository proyectos;
    @Autowired SolicitudCambioRepository solicitudes;
    @Autowired TareaRepository tareas;
    @Autowired HitoRepository hitos;
    @Autowired HitoTareaRepository vinculos;
    @Autowired AsignacionRepository asignaciones;
    @Autowired AsignacionService asignacionService;
    @Autowired RecursoService recursos;
    @Autowired CronogramaService cronograma;
    @Autowired RecalculoService recalculo;
    @Autowired SobrecargaAlertaRepository alertas;
    @Autowired SobrecargaAlertaTareaRepository alertaTareas;
    @Autowired CalendarioProyectos calendarios;

    private Proyecto proyecto;
    private Tarea revisarEspacio;
    private Tarea acordarAlcance;
    private Asignacion asignacionP2;
    private Recurso ingeniero;

    @BeforeEach
    void cargarSeed() {
        quitarSemillaSiExiste();
        assertThat(seed.cargar()).isTrue();

        proyecto = proyectos.findByCodigo(CODIGO_SEED).orElseThrow();
        revisarEspacio = porCodigo("1.1.2");
        acordarAlcance = porCodigo("1.2.1");
        asignacionP2 = asignaciones.findByTareaIdOrderById(revisarEspacio.id).stream()
            .filter(a -> a.cantidad.compareTo(new BigDecimal("45")) == 0)
            .findFirst()
            .orElseThrow();
        ingeniero = recursos.obtener(asignacionP2.recursoId);
        assertThat(ingeniero.nombre).isEqualTo("Ingeniero de infraestructura");
        assertThat(hito("H1").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hito("H2").estado).isEqualTo(EstadoHito.PENDIENTE);
    }

    /**
     * El seed no repite EEC-2026. La solicitud referencia la tarea sin ON DELETE,
     * así que hay que soltar ese vínculo antes de que la base borre el proyecto en cascada.
     */
    private void quitarSemillaSiExiste() {
        proyectos.findByCodigo(CODIGO_SEED).ifPresent(existente -> {
            solicitudes.findByProyectoId(existente.id).forEach(s -> s.tareaId = null);
            solicitudes.flush();
            proyectos.delete(existente);
            proyectos.flush();
        });
    }

    @Test
    void costosSobreasignacionEHitosQuedanRegistradosEnEseOrden() {
        List<String> nombres = pasos.stream()
            .map(p -> AopUtils.getTargetClass(p).getSimpleName())
            .toList();
        int costos = nombres.indexOf("PasoCostos");
        int sobreasignacion = nombres.indexOf("PasoSobreasignacion");
        int hitosPaso = nombres.indexOf("PasoHitos");
        assertThat(costos).isGreaterThanOrEqualTo(0);
        assertThat(costos).isLessThan(sobreasignacion);
        assertThat(sobreasignacion).isLessThan(hitosPaso);

        assertThat(ordenDe(pasos.get(costos))).isEqualTo(OrdenRecalculo.COSTOS);
        assertThat(ordenDe(pasos.get(sobreasignacion))).isEqualTo(OrdenRecalculo.SOBREASIGNACION);
        assertThat(ordenDe(pasos.get(hitosPaso))).isEqualTo(OrdenRecalculo.HITOS);
    }

    @Test
    void cambiarLaDuracionAdelantaElHitoYConservaCostoYAlerta() {
        assertThat(asignacionP2.costoPlan).isEqualByComparingTo(COSTO_45H);
        assertThat(revisarEspacio.costoPlan).isEqualByComparingTo(COSTO_45H);
        assertThat(alertasDeLaTarea(revisarEspacio.id)).isNotEmpty();

        Hito h1 = hito("H1");
        LocalDate fechaAntes = h1.fechaPlan;
        assertThat(h1.estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(fechaAntes).isAfterOrEqualTo(acordarAlcance.finPlan);

        cronograma.parchear(acordarAlcance.id, new TareaPatch(15, null, null, null));

        Tarea hoja = tareas.findById(acordarAlcance.id).orElseThrow();
        Hito despues = hito("H1");
        assertThat(hoja.finPlan).isAfter(fechaAntes);
        assertThat(despues.fechaPlan).isEqualTo(viernesDe(hoja.finPlan));
        assertThat(despues.fechaPlan).isAfterOrEqualTo(hoja.finPlan);
        assertThat(despues.estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(despues.fechaBase).isNull();
        assertThat(despues.fechaReal).isNull();

        Asignacion costo = asignaciones.findById(asignacionP2.id).orElseThrow();
        assertThat(costo.costoPlan).isEqualByComparingTo(COSTO_45H);
        assertThat(tareas.findById(revisarEspacio.id).orElseThrow().costoPlan).isEqualByComparingTo(COSTO_45H);
        assertThat(alertasDeLaTarea(revisarEspacio.id)).isNotEmpty();

        LocalDate fecha = despues.fechaPlan;
        recalculo.recalcular(proyecto.id);
        Hito otraVez = hito("H1");
        assertThat(otraVez.fechaPlan).isEqualTo(fecha);
        assertThat(otraVez.estado).isEqualTo(EstadoHito.PENDIENTE);
    }

    @Test
    void bajarLaAsignacionDeP2ActualizaElCostoYQuitaLaAlerta() {
        assertThat(asignacionP2.cantidad).isEqualByComparingTo("45");
        assertThat(asignacionP2.tarifa).isEqualByComparingTo("32000");
        assertThat(asignacionP2.costoPlan).isEqualByComparingTo(COSTO_45H);
        assertThat(alertasDeLaTarea(revisarEspacio.id)).isNotEmpty();

        asignacionService.actualizar(asignacionP2.id, new AsignacionRequest(
            revisarEspacio.id, ingeniero.id, new BigDecimal("40"), null, null, null));

        Asignacion actualizada = asignaciones.findById(asignacionP2.id).orElseThrow();
        assertThat(actualizada.costoPlan).isEqualByComparingTo(COSTO_40H);
        assertThat(actualizada.costoReal).isEqualByComparingTo("0");
        assertThat(tareas.findById(revisarEspacio.id).orElseThrow().costoPlan).isEqualByComparingTo(COSTO_40H);
        assertThat(alertasDeLaTarea(revisarEspacio.id)).isEmpty();

        Hito h1 = hito("H1");
        assertThat(h1.estado).isEqualTo(EstadoHito.PENDIENTE);

        recalculo.recalcular(proyecto.id);
        assertThat(asignaciones.findById(asignacionP2.id).orElseThrow().costoPlan).isEqualByComparingTo(COSTO_40H);
        assertThat(alertasDeLaTarea(revisarEspacio.id)).isEmpty();
        assertThat(hito("H1").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hito("H1").fechaPlan).isEqualTo(h1.fechaPlan);
    }

    @Test
    void unCumplidoQueDejaDeCumplirVuelveAPendienteYUnoValidoSeMantiene() {
        Tarea actividad = tareas.findById(acordarAlcance.id).orElseThrow();
        actividad.finReal = actividad.finPlan;
        tareas.saveAndFlush(actividad);

        Hito valido = hito("H1");
        LocalDate fechaPlan = valido.fechaPlan;
        valido.estado = EstadoHito.CUMPLIDO;
        valido.fechaReal = actividad.finReal;
        valido.evidencia = "Acta firmada";
        hitos.saveAndFlush(valido);

        Hito invalido = hito("H2");
        invalido.estado = EstadoHito.CUMPLIDO;
        invalido.fechaReal = null;
        hitos.saveAndFlush(invalido);

        recalculo.recalcular(proyecto.id);

        Hito h1 = hito("H1");
        assertThat(h1.estado).isEqualTo(EstadoHito.CUMPLIDO);
        assertThat(h1.fechaReal).isEqualTo(actividad.finReal);
        assertThat(h1.fechaPlan).isEqualTo(fechaPlan);
        assertThat(hito("H2").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hitos.findByProyectoIdOrderByFechaPlanAscCodigoAsc(proyecto.id))
            .filteredOn(h -> !"H1".equals(h.codigo))
            .allSatisfy(h -> assertThat(h.estado).isEqualTo(EstadoHito.PENDIENTE));

        LocalDate fechaRealAdelantada = actividad.finReal.minusDays(1);
        h1.estado = EstadoHito.CUMPLIDO;
        h1.fechaReal = fechaRealAdelantada;
        hitos.saveAndFlush(h1);
        recalculo.recalcular(proyecto.id);
        assertThat(hito("H1").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hito("H1").fechaReal).isEqualTo(fechaRealAdelantada);
        assertThat(hito("H1").fechaPlan).isEqualTo(fechaPlan);

        h1 = hito("H1");
        h1.estado = EstadoHito.CUMPLIDO;
        h1.fechaReal = actividad.finPlan;
        hitos.saveAndFlush(h1);
        actividad.finReal = null;
        tareas.saveAndFlush(actividad);
        recalculo.recalcular(proyecto.id);
        assertThat(hito("H1").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hito("H1").fechaReal).isEqualTo(actividad.finPlan);
        assertThat(hito("H1").fechaPlan).isEqualTo(fechaPlan);

        recalculo.recalcular(proyecto.id);
        assertThat(hito("H1").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hito("H2").estado).isEqualTo(EstadoHito.PENDIENTE);

        Hito sinActividades = hito("H1");
        vinculos.deleteByHitoId(sinActividades.id);
        vinculos.flush();
        sinActividades.estado = EstadoHito.CUMPLIDO;
        sinActividades.fechaReal = LocalDate.of(2026, 11, 6);
        sinActividades.evidencia = "Acta firmada";
        hitos.saveAndFlush(sinActividades);
        recalculo.recalcular(proyecto.id);
        assertThat(hito("H1").estado).isEqualTo(EstadoHito.PENDIENTE);
        assertThat(hito("H1").fechaReal).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    private List<SobrecargaAlerta> alertasDeLaTarea(Long tareaId) {
        List<SobrecargaAlerta> delProyecto =
            alertas.findByProyectoIdOrderByFechaAscRecursoNombreAsc(proyecto.id);
        return delProyecto.stream()
            .filter(alerta -> ingeniero.id.equals(alerta.recursoId))
            .filter(alerta -> alertaTareas.findByAlertaIdOrderByTareaCodigo(alerta.id).stream()
                .anyMatch(detalle -> tareaId.equals(detalle.tareaId)))
            .toList();
    }

    private LocalDate viernesDe(LocalDate fin) {
        Calendario calendario = calendarios.de(proyecto);
        int semana = SemanaHito.semanaDe(proyecto.fechaInicio, calendario, fin);
        return SemanaHito.fechaDeSemana(proyecto.fechaInicio, calendario, semana);
    }

    private Hito hito(String codigo) {
        return hitos.findByProyectoIdOrderByFechaPlanAscCodigoAsc(proyecto.id).stream()
            .filter(h -> codigo.equals(h.codigo))
            .findFirst()
            .orElseThrow();
    }

    private Tarea porCodigo(String codigo) {
        return tareas.findByProyectoId(proyecto.id).stream()
            .filter(t -> codigo.equals(t.codigo))
            .findFirst()
            .orElseThrow();
    }

    private static int ordenDe(PasoRecalculo paso) {
        Order order = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(paso), Order.class);
        assertThat(order).isNotNull();
        return order.value();
    }
}
