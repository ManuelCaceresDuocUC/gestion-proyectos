package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.hito.EstadoHito;
import cl.proyecto.gestion.hito.Hito;
import cl.proyecto.gestion.hito.HitoRepository;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlerta;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaRepository;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TareaRequest;
import cl.proyecto.gestion.proyecto.TareaService;
import cl.proyecto.gestion.proyecto.TipoTarea;
import cl.proyecto.gestion.recurso.Asignacion;
import cl.proyecto.gestion.recurso.AsignacionRepository;
import cl.proyecto.gestion.recurso.Recurso;
import cl.proyecto.gestion.recurso.RecursoRequest;
import cl.proyecto.gestion.recurso.RecursoService;
import cl.proyecto.gestion.recurso.TipoRecurso;
import cl.proyecto.gestion.recurso.TipoRecursoRepository;
import cl.proyecto.gestion.recurso.TipoRecursoRequest;
import cl.proyecto.gestion.recurso.TipoRecursoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class PasoKpiTest {

    @Autowired MockMvc mvc;
    @Autowired PasoKpi paso;
    @Autowired RecalculoService recalculo;
    @Autowired KpiService kpi;
    @Autowired KpiDefinicionRepository definiciones;
    @Autowired KpiVariableRepository variablesKpi;
    @Autowired KpiHistoricoRepository historicos;
    @Autowired ProyectoService proyectos;
    @Autowired ProyectoRepository proyectoRepo;
    @Autowired TareaRepository tareas;
    @Autowired TareaService tareaService;
    @Autowired HitoRepository hitos;
    @Autowired TipoRecursoService tipos;
    @Autowired TipoRecursoRepository tipoRepo;
    @Autowired RecursoService recursos;
    @Autowired AsignacionRepository asignaciones;
    @Autowired SobrecargaAlertaRepository alertas;

    @Test
    void guardaLosOchoKpiConValoresDelProyectoYAlertaSiEmpeora() throws Exception {
        Proyecto proyecto = escenario();

        paso.ejecutar(proyecto.id);

        assertThat(definiciones.findByProyectoIdIsNullAndActivoTrueOrderByCodigoAsc())
                .extracting(d -> d.codigo)
                .containsExactly("KP-01", "KP-02", "KP-03", "KP-04", "KP-05", "KP-06", "KP-07", "KP-08");
        KpiDefinicion kp01 = definiciones.findByProyectoIdIsNullAndCodigo("KP-01").orElseThrow();
        assertThat(kp01.meta).isNull();
        assertThat(variablesKpi.findByKpiIdOrderByNombreAsc(kp01.id))
                .extracting(v -> v.nombre)
                .containsExactly("AVANCE_REAL");
        assertThat(historicos.findByProyectoId(proyecto.id)).hasSize(8);

        assertValor("KP-01", proyecto.id, "50.00", "0.5000", SemaforoKpi.ROJO);
        assertValor("KP-02", proyecto.id, "0.50", "0.5000", SemaforoKpi.ROJO);
        assertValor("KP-03", proyecto.id, "0.50", "0.5000", SemaforoKpi.ROJO);
        assertValor("KP-04", proyecto.id, "1.00", "0.0000", SemaforoKpi.ROJO);
        assertValor("KP-05", proyecto.id, "100.00", "0.1500", SemaforoKpi.ROJO);
        assertValor("KP-06", proyecto.id, "50.00", "0.5556", SemaforoKpi.ROJO);
        assertValor("KP-07", proyecto.id, "0.00", "1.0000", SemaforoKpi.VERDE);
        assertValor("KP-08", proyecto.id, "50.00", "0.0000", SemaforoKpi.ROJO);

        KpiEstadoResponse desviacion = estado(proyecto.id, "KP-07");
        assertThat(desviacion.alerta()).isFalse();
        assertThat(estado(proyecto.id, "KP-01").alerta()).isTrue();
        assertThat(estado(proyecto.id, "KP-01").alertaMotivo()).isEqualTo(AlertaKpi.ROJO);

        paso.ejecutar(proyecto.id);
        assertThat(historicos.findByProyectoId(proyecto.id)).hasSize(16);
        assertThat(estado(proyecto.id, "KP-07").alerta()).isFalse();
        assertThat(ultimo("KP-01", proyecto.id).valor).isEqualByComparingTo("50.00");

        proyecto.costoReal = new BigDecimal("1100.00");
        proyectoRepo.save(proyecto);
        paso.ejecutar(proyecto.id);

        assertValor("KP-07", proyecto.id, "10.00", "0.5000", SemaforoKpi.ROJO);
        assertThat(estado(proyecto.id, "KP-07").alertaMotivo()).isEqualTo(AlertaKpi.ROJO);
        assertThat(historicos.findByProyectoId(proyecto.id)).hasSize(24);

        mvc.perform(get("/api/proyectos/{id}/kpi", proyecto.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[6].codigo").value("KP-07"))
                .andExpect(jsonPath("$[6].alertaMotivo").value(AlertaKpi.ROJO))
                .andExpect(jsonPath("$[3].codigo").value("KP-04"))
                .andExpect(jsonPath("$[3].motivo").value(KpiService.MOTIVO_RECALCULO));

        mvc.perform(get("/api/kpi/{id}/historico", definicion("KP-07").id)
                        .param("proyectoId", proyecto.id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].valor").value(10.0))
                .andExpect(jsonPath("$[0].semaforo").value("ROJO"))
                .andExpect(jsonPath("$[0].noCalculable").value(false))
                .andExpect(jsonPath("$[2].valor").value(0.0))
                .andExpect(jsonPath("$[2].semaforo").value("VERDE"));

        mvc.perform(get("/api/kpi/{id}/historico", definicion("KP-07").id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PROYECTO_OBLIGATORIO"));

        mvc.perform(get("/api/kpi/{id}/historico", 999_999L).param("proyectoId", proyecto.id.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(roles = "USUARIO_PROYECTO")
    void unUsuarioDeProyectoPuedeLeerLaSerie() throws Exception {
        Proyecto proyecto = escenario();
        paso.ejecutar(proyecto.id);
        mvc.perform(get("/api/proyectos/{id}/kpi", proyecto.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void elRecalculoCompletoDejaSnapshotYRepiteLosValores() {
        Proyecto proyecto = proyectos.crear(new ProyectoRequest(
                "KPI-CAD-" + System.nanoTime(), "Cadena", null, null, null,
                new BigDecimal("1000"), null, "LMXJV", LocalDate.of(2026, 11, 2)));
        Tarea etapa = tareaService.crear(proyecto.id, new TareaRequest(null, "Etapa", null, null, null, null));
        tareaService.crear(proyecto.id, new TareaRequest(etapa.id, "Hoja", null, 2, null, null));

        int despues = historicos.findByProyectoId(proyecto.id).size();
        assertThat(despues).isGreaterThanOrEqualTo(8);
        assertThat(despues % 8).isZero();
        assertThat(historicos.findByProyectoId(proyecto.id))
                .allMatch(fila -> KpiService.MOTIVO_RECALCULO.equals(fila.motivo));

        recalculo.recalcular(proyecto.id);
        assertThat(historicos.findByProyectoId(proyecto.id)).hasSize(despues + 8);
    }

    @Test
    void reponerVariablesSiLaDefinicionGlobalYaExiste() {
        CatalogoKpiProyecto.Item item = CatalogoKpiProyecto.porCodigo("KP-02");
        KpiDefinicion definicion = new KpiDefinicion();
        definicion.codigo = item.codigo();
        definicion.nombre = item.nombre();
        definicion.categoria = item.categoria();
        definicion.formula = item.formula();
        definicion.unidad = item.unidad();
        definicion.sentido = item.sentido();
        definicion.operadorMeta = item.operador();
        definicion.meta = item.meta();
        definicion.umbralVerde = BigDecimal.ONE;
        definicion.umbralAmarillo = new BigDecimal("0.8000");
        definicion.activo = true;
        definicion = definiciones.saveAndFlush(definicion);
        KpiVariable presente = new KpiVariable();
        presente.kpiId = definicion.id;
        presente.nombre = "VALOR_GANADO";
        variablesKpi.saveAndFlush(presente);

        kpi.asegurarCatalogo();
        assertThat(variablesKpi.findByKpiIdOrderByNombreAsc(definicion.id))
                .extracting(v -> v.nombre)
                .containsExactly("VALOR_GANADO", "VALOR_PLANIFICADO");

        kpi.asegurarCatalogo();
        assertThat(variablesKpi.findByKpiIdOrderByNombreAsc(definicion.id)).hasSize(2);
        assertThat(definiciones.findByProyectoIdIsNullAndCodigo("KP-02")).isPresent();
    }

    @Test
    void sinFinPlanElAtrasoQuedaNoCalculable() {
        Proyecto proyecto = proyectos.crear(new ProyectoRequest(
                "KPI-FIN-" + System.nanoTime(), "Sin fin", null, null, null,
                BigDecimal.ZERO, null, "LMXJV", LocalDate.of(2026, 11, 2)));
        proyecto.fechaFinBase = LocalDate.of(2026, 11, 6);
        proyecto.fechaFinPlan = null;
        proyectoRepo.save(proyecto);

        paso.ejecutar(proyecto.id);

        KpiHistorico plazo = ultimo("KP-04", proyecto.id);
        assertThat(plazo.valor).isNull();
        assertThat(plazo.semaforo).isNull();
        assertThat(plazo.motivo).isEqualTo(KpiService.MOTIVO_RECALCULO);
        assertThat(estado(proyecto.id, "KP-04").noCalculable()).isTrue();
        assertThat(estado(proyecto.id, "KP-04").alerta()).isFalse();
    }

    private Proyecto escenario() {
        Proyecto proyecto = proyectos.crear(new ProyectoRequest(
                "KPI-" + System.nanoTime(), "Medición", null, null, null,
                new BigDecimal("1000.00"), null, "LMXJV", LocalDate.of(2000, 1, 3)));
        proyecto.avance = new BigDecimal("50.00");
        proyecto.costoPlan = new BigDecimal("1000.00");
        proyecto.costoReal = new BigDecimal("1000.00");
        proyecto.fechaFinBase = LocalDate.of(2000, 1, 6);
        proyecto.fechaFinPlan = LocalDate.of(2000, 1, 7);
        proyecto = proyectoRepo.save(proyecto);

        Tarea etapa = new Tarea();
        etapa.proyectoId = proyecto.id;
        etapa.tipo = TipoTarea.ETAPA;
        etapa.codigo = "1";
        etapa.nombre = "Etapa";
        etapa.orden = 1;
        etapa.duracionDias = 5;
        etapa.avance = new BigDecimal("50.00");
        etapa.costoPlan = BigDecimal.ZERO;
        etapa.costoReal = BigDecimal.ZERO;
        etapa = tareas.save(etapa);

        Tarea hoja = new Tarea();
        hoja.proyectoId = proyecto.id;
        hoja.parentId = etapa.id;
        hoja.tipo = TipoTarea.SUBACTIVIDAD;
        hoja.codigo = "1.1";
        hoja.nombre = "Hoja";
        hoja.orden = 1;
        hoja.duracionDias = 5;
        hoja.inicioPlan = LocalDate.of(2000, 1, 3);
        hoja.finPlan = LocalDate.of(2000, 1, 7);
        hoja.avance = new BigDecimal("50.00");
        hoja.costoPlan = new BigDecimal("1000.00");
        hoja.costoReal = new BigDecimal("1000.00");
        hoja = tareas.save(hoja);

        TipoRecurso profesional = tipoRepo.findByNombreIgnoreCase(VariablesProyecto.TIPO_PROFESIONAL)
                .orElseGet(() -> tipos.crear(new TipoRecursoRequest(VariablesProyecto.TIPO_PROFESIONAL, "hora")));
        TipoRecurso material = tipos.crear(new TipoRecursoRequest("Material-" + System.nanoTime(), "unidad"));
        Recurso uno = recursos.crear(new RecursoRequest(
                profesional.id, "P1-" + System.nanoTime(), new BigDecimal("10"), new BigDecimal("8.0"), true));
        Recurso dos = recursos.crear(new RecursoRequest(
                profesional.id, "P2-" + System.nanoTime(), new BigDecimal("10"), new BigDecimal("8.0"), true));
        Recurso insumo = recursos.crear(new RecursoRequest(
                material.id, "M-" + System.nanoTime(), new BigDecimal("1"), null, true));
        asignar(hoja.id, uno.id);
        asignar(hoja.id, dos.id);
        asignar(hoja.id, insumo.id);
        alerta(proyecto.id, uno, LocalDate.of(2000, 1, 4));
        alerta(proyecto.id, uno, LocalDate.of(2000, 1, 5));

        Hito aTiempo = hito(proyecto.id, "H1", LocalDate.of(2000, 1, 4), LocalDate.of(2000, 1, 4), EstadoHito.CUMPLIDO);
        Hito vencido = hito(proyecto.id, "H2", LocalDate.of(2000, 1, 5), null, EstadoHito.PENDIENTE);
        Hito futuro = hito(proyecto.id, "H3", LocalDate.of(2099, 1, 1), null, EstadoHito.PENDIENTE);
        hitos.saveAll(List.of(aTiempo, vencido, futuro));
        return proyecto;
    }

    private void asignar(Long tareaId, Long recursoId) {
        Asignacion asignacion = new Asignacion();
        asignacion.tareaId = tareaId;
        asignacion.recursoId = recursoId;
        asignacion.cantidad = new BigDecimal("8.00");
        asignacion.tarifa = new BigDecimal("10.00");
        asignacion.cantidadReal = BigDecimal.ZERO;
        asignacion.costoPlan = BigDecimal.ZERO;
        asignacion.costoReal = BigDecimal.ZERO;
        asignaciones.save(asignacion);
    }

    private void alerta(Long proyectoId, Recurso recurso, LocalDate fecha) {
        SobrecargaAlerta alerta = new SobrecargaAlerta();
        alerta.proyectoId = proyectoId;
        alerta.recursoId = recurso.id;
        alerta.recursoNombre = recurso.nombre;
        alerta.fecha = fecha;
        alerta.cargaHoras = new BigDecimal("10.0000");
        alerta.capacidadHorasDia = new BigDecimal("8.0");
        alerta.excesoHoras = new BigDecimal("2.0000");
        alertas.save(alerta);
    }

    private static Hito hito(Long proyectoId, String codigo, LocalDate plan, LocalDate real, EstadoHito estado) {
        Hito hito = new Hito();
        hito.proyectoId = proyectoId;
        hito.codigo = codigo;
        hito.logro = codigo;
        hito.fechaPlan = plan;
        hito.fechaReal = real;
        hito.estado = estado;
        return hito;
    }

    private void assertValor(String codigo, Long proyectoId, String valor, String cumplimiento, SemaforoKpi semaforo) {
        KpiHistorico fila = ultimo(codigo, proyectoId);
        assertThat(fila.valor).isEqualByComparingTo(valor);
        assertThat(fila.cumplimiento).isEqualByComparingTo(cumplimiento);
        assertThat(fila.semaforo).isEqualTo(semaforo);
        assertThat(fila.motivo).isEqualTo(KpiService.MOTIVO_RECALCULO);
    }

    private KpiHistorico ultimo(String codigo, Long proyectoId) {
        return historicos.findTop2ByKpiIdAndProyectoIdOrderByCalculadoEnDescIdDesc(
                definicion(codigo).id, proyectoId).get(0);
    }

    private KpiDefinicion definicion(String codigo) {
        return definiciones.findByProyectoIdIsNullAndCodigo(codigo).orElseThrow();
    }

    private KpiEstadoResponse estado(Long proyectoId, String codigo) {
        return kpi.listar(proyectoId).stream()
                .filter(fila -> codigo.equals(fila.codigo()))
                .findFirst()
                .orElseThrow();
    }
}
