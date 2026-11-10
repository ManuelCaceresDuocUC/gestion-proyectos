package cl.proyecto.gestion.tablero;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.proyecto.gestion.hito.EstadoHito;
import cl.proyecto.gestion.hito.Hito;
import cl.proyecto.gestion.hito.HitoRepository;
import cl.proyecto.gestion.kpi.CategoriaKpi;
import cl.proyecto.gestion.kpi.KpiDefinicion;
import cl.proyecto.gestion.kpi.KpiDefinicionRepository;
import cl.proyecto.gestion.kpi.KpiHistorico;
import cl.proyecto.gestion.kpi.KpiHistoricoRepository;
import cl.proyecto.gestion.kpi.KpiService;
import cl.proyecto.gestion.kpi.OperadorMeta;
import cl.proyecto.gestion.kpi.SemaforoKpi;
import cl.proyecto.gestion.kpi.SentidoKpi;
import cl.proyecto.gestion.planificacion.recalculo.RecalculoService;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import cl.proyecto.gestion.proyecto.ProyectoRequest;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.proyecto.TipoTarea;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** B-21: el tablero lee lo ya guardado y no dispara el recálculo ni un snapshot nuevo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "USUARIO_PROYECTO")
class TableroProyectoApiTest {

    @Autowired MockMvc mvc;
    @Autowired ProyectoService proyectos;
    @Autowired ProyectoRepository proyectoRepo;
    @Autowired TareaRepository tareas;
    @Autowired HitoRepository hitos;
    @Autowired KpiDefinicionRepository definiciones;
    @Autowired KpiHistoricoRepository historicos;
    @MockitoSpyBean RecalculoService recalculo;
    @MockitoSpyBean KpiService kpi;

    @Test
    void proyectoInexistente() throws Exception {
        mvc.perform(get("/api/tablero/proyecto/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void leeHistoricoHitosYSoloLaHojaAtrasada() throws Exception {
        LocalDate hoy = LocalDate.now();
        Proyecto proyecto = proyectos.crear(new ProyectoRequest(
                "B21-TAB", "Tablero", null, null, null,
                new BigDecimal("1500.00"), "CLP", "LMXJV", LocalDate.of(2026, 1, 5)));
        proyecto.avance = new BigDecimal("33.00");
        proyecto.costoPlan = new BigDecimal("800.00");
        proyecto.costoReal = new BigDecimal("500.00");
        proyectoRepo.save(proyecto);

        Tarea etapa = tarea(proyecto.id, null, TipoTarea.ETAPA, "1", "Etapa", hoy.minusDays(3), new BigDecimal("10.00"));
        Tarea actividad = tarea(proyecto.id, etapa.id, TipoTarea.ACTIVIDAD, "1.1", "Actividad",
                hoy.minusDays(2), new BigDecimal("20.00"));
        tarea(proyecto.id, actividad.id, TipoTarea.SUBACTIVIDAD, "1.1.1", "Hoja atrasada",
                hoy.minusDays(1), new BigDecimal("40.00"));
        tarea(proyecto.id, actividad.id, TipoTarea.SUBACTIVIDAD, "1.1.2", "Hoja terminada",
                hoy.minusDays(1), new BigDecimal("100.00"));

        hito(proyecto.id, "H-OK", EstadoHito.CUMPLIDO, hoy.minusDays(5), hoy.minusDays(4));
        hito(proyecto.id, "H-VENC", EstadoHito.PENDIENTE, hoy.minusDays(1), null);
        hito(proyecto.id, "H-NULO", EstadoHito.PENDIENTE, null, null);

        definicion(proyecto.id, "KP-TAB", CategoriaKpi.PROYECTO, new BigDecimal("12.3400"), SemaforoKpi.VERDE);
        definicion(proyecto.id, "KH-TAB", CategoriaKpi.HITOS, new BigDecimal("7.0000"), SemaforoKpi.AMARILLO);
        definicion(proyecto.id, "KC-TAB", CategoriaKpi.CAMBIO, new BigDecimal("1.0000"), SemaforoKpi.ROJO);
        long snapshots = historicos.count();

        clearInvocations(recalculo, kpi);
        mvc.perform(get("/api/tablero/proyecto/{id}", proyecto.id)
                        .param("etapaId", "1")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-12-31")
                        .param("responsableId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("B21-TAB"))
                .andExpect(jsonPath("$.avance.real").value(33.00))
                .andExpect(jsonPath("$.avance.planificado").value(100.00))
                .andExpect(jsonPath("$.costos.plan").value(800.00))
                .andExpect(jsonPath("$.costos.real").value(500.00))
                .andExpect(jsonPath("$.costos.presupuesto").value(1500.00))
                .andExpect(jsonPath("$.costos.moneda").value("CLP"))
                .andExpect(jsonPath("$.actividadesAtrasadas.length()").value(1))
                .andExpect(jsonPath("$.actividadesAtrasadas[0].codigo").value("1.1.1"))
                .andExpect(jsonPath("$.hitos.cumplidos[*].codigo", hasItem("H-OK")))
                .andExpect(jsonPath("$.hitos.vencidos[*].codigo", hasItem("H-VENC")))
                .andExpect(jsonPath("$.hitos.proximos[*].codigo", hasItem("H-NULO")))
                .andExpect(jsonPath("$.hitos.vencidos[*].codigo", not(hasItem("H-OK"))))
                .andExpect(jsonPath("$.hitos.vencidos[*].codigo", not(hasItem("H-NULO"))))
                .andExpect(jsonPath("$.kpi[*].codigo", hasItem("KP-TAB")))
                .andExpect(jsonPath("$.kpi[*].codigo", hasItem("KH-TAB")))
                .andExpect(jsonPath("$.kpi[*].codigo", not(hasItem("KC-TAB"))))
                .andExpect(jsonPath("$.kpi[*].categoria", not(hasItem("CAMBIO"))))
                .andExpect(jsonPath("$.kpi[?(@.codigo=='KP-TAB')].valor", hasItem(12.34)))
                .andExpect(jsonPath("$.kpi[?(@.codigo=='KP-TAB')].semaforo", hasItem("VERDE")));

        org.assertj.core.api.Assertions.assertThat(historicos.count()).isEqualTo(snapshots);
        verify(recalculo, never()).recalcular(anyLong());
        verify(kpi, never()).registrarSnapshot(anyLong());
    }

    private Tarea tarea(Long proyectoId, Long parentId, TipoTarea tipo, String codigo, String nombre,
                        LocalDate finPlan, BigDecimal avance) {
        Tarea tarea = new Tarea();
        tarea.proyectoId = proyectoId;
        tarea.parentId = parentId;
        tarea.tipo = tipo;
        tarea.codigo = codigo;
        tarea.nombre = nombre;
        tarea.orden = 1;
        tarea.duracionDias = 5;
        tarea.inicioPlan = finPlan.minusDays(6);
        tarea.finPlan = finPlan;
        tarea.avance = avance;
        return tareas.save(tarea);
    }

    private void hito(Long proyectoId, String codigo, EstadoHito estado, LocalDate fechaPlan, LocalDate fechaReal) {
        Hito hito = new Hito();
        hito.proyectoId = proyectoId;
        hito.codigo = codigo;
        hito.logro = codigo;
        hito.estado = estado;
        hito.fechaPlan = fechaPlan;
        hito.fechaReal = fechaReal;
        hitos.save(hito);
    }

    private void definicion(Long proyectoId, String codigo, CategoriaKpi categoria,
                            BigDecimal valor, SemaforoKpi semaforo) {
        KpiDefinicion definicion = new KpiDefinicion();
        definicion.proyectoId = proyectoId;
        definicion.codigo = codigo;
        definicion.nombre = codigo;
        definicion.categoria = categoria;
        definicion.formula = "1";
        definicion.unidad = "%";
        definicion.sentido = SentidoKpi.MAYOR_MEJOR;
        definicion.operadorMeta = OperadorMeta.MAYOR_IGUAL;
        definicion.meta = new BigDecimal("80");
        definiciones.save(definicion);

        KpiHistorico fila = new KpiHistorico();
        fila.kpiId = definicion.id;
        fila.proyectoId = proyectoId;
        fila.valor = valor;
        fila.cumplimiento = new BigDecimal("1.0000");
        fila.semaforo = semaforo;
        fila.motivo = "RECALCULO";
        fila.calculadoEn = Instant.parse("2026-11-01T12:00:00Z");
        historicos.save(fila);
    }
}
