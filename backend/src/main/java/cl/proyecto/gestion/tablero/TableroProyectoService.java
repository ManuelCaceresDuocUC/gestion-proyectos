package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.hito.EstadoHito;
import cl.proyecto.gestion.hito.Hito;
import cl.proyecto.gestion.hito.HitoRepository;
import cl.proyecto.gestion.hito.HitoTarea;
import cl.proyecto.gestion.hito.HitoTareaRepository;
import cl.proyecto.gestion.kpi.CategoriaKpi;
import cl.proyecto.gestion.kpi.KpiEstadoResponse;
import cl.proyecto.gestion.kpi.KpiService;
import cl.proyecto.gestion.kpi.VariablesProyecto;
import cl.proyecto.gestion.kpi.VariablesProyectoService;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaResponse;
import cl.proyecto.gestion.planificacion.recalculo.SobrecargaAlertaService;
import cl.proyecto.gestion.proyecto.Proyecto;
import cl.proyecto.gestion.proyecto.ProyectoService;
import cl.proyecto.gestion.proyecto.Tarea;
import cl.proyecto.gestion.proyecto.TareaRepository;
import cl.proyecto.gestion.tablero.TableroProyectoResponse.ActividadAtrasada;
import cl.proyecto.gestion.tablero.TableroProyectoResponse.Avance;
import cl.proyecto.gestion.tablero.TableroProyectoResponse.Costos;
import cl.proyecto.gestion.tablero.TableroProyectoResponse.HitoResumen;
import cl.proyecto.gestion.tablero.TableroProyectoResponse.Hitos;
import cl.proyecto.gestion.tablero.TableroProyectoResponse.Recursos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura del tablero de proyecto (B-21 / C-13). No recalcula ni evalúa fórmulas.
 * Los query recortan hitos, atrasos y alertas; avance, costos y KPI siguen siendo del proyecto.
 */
@Service
public class TableroProyectoService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ProyectoService proyectos;
    private final TareaRepository tareas;
    private final HitoRepository hitos;
    private final HitoTareaRepository vinculos;
    private final VariablesProyectoService mediciones;
    private final SobrecargaAlertaService sobrecargas;
    private final KpiService kpi;

    public TableroProyectoService(
            ProyectoService proyectos,
            TareaRepository tareas,
            HitoRepository hitos,
            HitoTareaRepository vinculos,
            VariablesProyectoService mediciones,
            SobrecargaAlertaService sobrecargas,
            KpiService kpi) {
        this.proyectos = proyectos;
        this.tareas = tareas;
        this.hitos = hitos;
        this.vinculos = vinculos;
        this.mediciones = mediciones;
        this.sobrecargas = sobrecargas;
        this.kpi = kpi;
    }

    @Transactional(readOnly = true)
    public TableroProyectoResponse obtener(Long proyectoId) {
        return obtener(proyectoId, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public TableroProyectoResponse obtener(
            Long proyectoId, Long etapaId, LocalDate desde, LocalDate hasta, Long responsableId) {
        LocalDate hoy = LocalDate.now();
        Proyecto proyecto = proyectos.obtener(proyectoId);
        List<Tarea> edt = tareas.findByProyectoId(proyectoId);
        Set<Long> rama = ramaDe(etapaId, edt);
        VariablesProyecto.Medicion medicion = mediciones.deProyecto(proyectoId, hoy);
        return new TableroProyectoResponse(
                proyecto.id,
                proyecto.codigo,
                proyecto.nombre,
                new Avance(proyecto.avance, puntos(medicion.valores().get(VariablesProyecto.AVANCE_PLAN))),
                clasificarHitos(proyectoId, hoy, rama, desde, hasta, responsableId),
                new Costos(proyecto.costoPlan, proyecto.costoReal, proyecto.presupuesto, proyecto.moneda),
                new Recursos(
                        entero(medicion, VariablesProyecto.PROFESIONALES_TOTAL),
                        entero(medicion, VariablesProyecto.PROFESIONALES_SOBREASIGNADOS),
                        FiltroTablero.alertas(sobrecargas.listar(proyectoId), rama, desde, hasta, responsableId)),
                actividadesAtrasadas(edt, hoy, rama, desde, hasta, responsableId),
                kpiDeProyectoEHitos(proyectoId));
    }

    private static Set<Long> ramaDe(Long etapaId, List<Tarea> edt) {
        if (etapaId == null) {
            return null;
        }
        Set<Long> rama = FiltroTablero.rama(etapaId, edt);
        if (rama.isEmpty()) {
            throw ApiException.noEncontrado("No existe la etapa " + etapaId + " en el proyecto.");
        }
        return rama;
    }

    private Hitos clasificarHitos(
            Long proyectoId, LocalDate hoy, Set<Long> rama, LocalDate desde, LocalDate hasta, Long responsableId) {
        List<Hito> lista = hitos.findByProyectoIdOrderByFechaPlanAscCodigoAsc(proyectoId);
        List<Long> ids = lista.stream().map(h -> h.id).toList();
        Map<Long, List<Long>> tareasPorHito = ids.isEmpty()
                ? Map.of()
                : vinculos.findByHitoIdIn(ids).stream()
                        .collect(Collectors.groupingBy(
                                v -> v.hitoId, Collectors.mapping(v -> v.tareaId, Collectors.toList())));
        lista = FiltroTablero.hitos(lista, rama, desde, hasta, responsableId, tareasPorHito);
        List<HitoResumen> cumplidos = new ArrayList<>();
        List<HitoResumen> proximos = new ArrayList<>();
        List<HitoResumen> vencidos = new ArrayList<>();
        for (Hito hito : lista) {
            HitoResumen resumen = new HitoResumen(
                    hito.id, hito.codigo, hito.logro, hito.fechaPlan, hito.fechaReal, hito.estado.name());
            if (hito.estado == EstadoHito.CUMPLIDO) {
                cumplidos.add(resumen);
            } else if (hito.fechaPlan != null && hito.fechaPlan.isBefore(hoy)) {
                vencidos.add(resumen);
            } else {
                proximos.add(resumen);
            }
        }
        return new Hitos(cumplidos, proximos, vencidos);
    }

    private List<ActividadAtrasada> actividadesAtrasadas(
            List<Tarea> edt, LocalDate hoy, Set<Long> rama, LocalDate desde, LocalDate hasta, Long responsableId) {
        Set<Long> conHijos = new HashSet<>();
        for (Tarea tarea : edt) {
            if (tarea.parentId != null) {
                conHijos.add(tarea.parentId);
            }
        }
        List<ActividadAtrasada> atrasadas = new ArrayList<>();
        for (Tarea tarea : edt) {
            if (VariablesProyecto.hojaAtrasada(conHijos.contains(tarea.id), tarea.finPlan, tarea.avance, hoy)
                    && FiltroTablero.tareaPasa(tarea, rama, desde, hasta, responsableId)) {
                atrasadas.add(new ActividadAtrasada(
                        tarea.id, tarea.codigo, tarea.nombre, tarea.finPlan, tarea.avance));
            }
        }
        atrasadas.sort(Comparator.comparing(ActividadAtrasada::codigo));
        return atrasadas;
    }

    private List<KpiEstadoResponse> kpiDeProyectoEHitos(Long proyectoId) {
        return kpi.listar(proyectoId).stream()
                .filter(item -> CategoriaKpi.PROYECTO.name().equals(item.categoria())
                        || CategoriaKpi.HITOS.name().equals(item.categoria()))
                .toList();
    }

    private static int entero(VariablesProyecto.Medicion medicion, String nombre) {
        BigDecimal valor = medicion.valores().get(nombre);
        return valor == null ? 0 : valor.intValue();
    }

    /** La medición guarda el avance planificado como razón 0–1. El tablero lo muestra en puntos. */
    private static BigDecimal puntos(BigDecimal razon) {
        if (razon == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return razon.multiply(CIEN).setScale(2, RoundingMode.HALF_UP);
    }
}
