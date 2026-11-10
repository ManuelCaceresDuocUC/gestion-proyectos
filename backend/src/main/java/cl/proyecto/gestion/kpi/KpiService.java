package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga KP-01…KP-08 y KC-01…KC-13, evalúa las definiciones activas y guarda el snapshot.
 * Las definiciones de cambio se insertan en el recálculo, no en un GET.
 */
@Service
public class KpiService {

    static final String MOTIVO_RECALCULO = "RECALCULO";

    private final KpiDefinicionRepository definiciones;
    private final KpiVariableRepository variables;
    private final KpiHistoricoRepository historicos;
    private final ProyectoRepository proyectos;
    private final VariablesProyectoService variablesProyecto;
    private final VariablesCambioService variablesCambio;
    private final EvaluadorKpi evaluador = new EvaluadorKpi();

    public KpiService(
            KpiDefinicionRepository definiciones,
            KpiVariableRepository variables,
            KpiHistoricoRepository historicos,
            ProyectoRepository proyectos,
            VariablesProyectoService variablesProyecto,
            VariablesCambioService variablesCambio) {
        this.definiciones = definiciones;
        this.variables = variables;
        this.historicos = historicos;
        this.proyectos = proyectos;
        this.variablesProyecto = variablesProyecto;
        this.variablesCambio = variablesCambio;
    }

    @Transactional
    public void registrarSnapshot(Long proyectoId) {
        if (proyectoId == null || !proyectos.existsById(proyectoId)) {
            return;
        }
        asegurarCatalogo();
        LocalDate hoy = LocalDate.now();
        VariablesProyecto.Medicion medicion = variablesProyecto.deProyecto(proyectoId, hoy);
        Map<String, BigDecimal> valores = new LinkedHashMap<>(medicion.valores());
        valores.putAll(variablesCambio.deProyecto(proyectoId, hoy));
        Instant ahora = Instant.now();
        for (KpiDefinicion definicion : aplicables(proyectoId)) {
            ResultadoKpi resultado = evaluar(definicion, valores, medicion.noCalculables());
            KpiHistorico fila = new KpiHistorico();
            fila.kpiId = definicion.id;
            fila.proyectoId = proyectoId;
            fila.valor = resultado.valor();
            fila.cumplimiento = resultado.cumplimiento();
            fila.semaforo = resultado.semaforo();
            fila.motivo = MOTIVO_RECALCULO;
            fila.calculadoEn = ahora;
            historicos.save(fila);
        }
    }

    @Transactional(readOnly = true)
    public List<KpiEstadoResponse> listar(Long proyectoId) {
        exigirProyecto(proyectoId);
        List<KpiEstadoResponse> respuesta = new ArrayList<>();
        for (KpiDefinicion definicion : aplicables(proyectoId)) {
            List<KpiHistorico> recientes = historicos
                    .findTop2ByKpiIdAndProyectoIdOrderByCalculadoEnDescIdDesc(definicion.id, proyectoId);
            KpiHistorico ultimo = recientes.isEmpty() ? null : recientes.get(0);
            KpiHistorico anterior = recientes.size() < 2 ? null : recientes.get(1);
            String alerta = ultimo == null
                    ? null
                    : AlertaKpi.motivo(
                            ultimo.semaforo,
                            ultimo.cumplimiento,
                            anterior == null ? null : anterior.cumplimiento);
            respuesta.add(estado(definicion, ultimo, alerta));
        }
        return respuesta;
    }

    @Transactional(readOnly = true)
    public List<KpiHistoricoResponse> historico(Long kpiId, Long proyectoId) {
        if (proyectoId == null) {
            throw ApiException.invalido("PROYECTO_OBLIGATORIO", "Indique el proyecto de la serie histórica.");
        }
        KpiDefinicion definicion = definiciones.findById(kpiId)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el KPI " + kpiId + "."));
        exigirProyecto(proyectoId);
        if (definicion.proyectoId != null && !definicion.proyectoId.equals(proyectoId)) {
            throw ApiException.noEncontrado("No existe el KPI " + kpiId + " en el proyecto " + proyectoId + ".");
        }
        return historicos.findByKpiIdAndProyectoIdOrderByCalculadoEnDescIdDesc(kpiId, proyectoId).stream()
                .map(KpiService::historico)
                .toList();
    }

    @Transactional
    public void asegurarCatalogo() {
        for (CatalogoKpiProyecto.Item item : CatalogoKpiProyecto.todos()) {
            asegurarDefinicion(
                    item.codigo(), item.nombre(), item.categoria(), item.descripcion(),
                    item.formula(), item.unidad(), item.sentido(), item.operador(),
                    item.meta(), item.responsable());
        }
        for (CatalogoKpiCambio.Item item : CatalogoKpiCambio.todos()) {
            asegurarDefinicion(
                    item.codigo(), item.nombre(), item.categoria(), item.descripcion(),
                    item.formula(), item.unidad(), item.sentido(), item.operador(),
                    item.meta(), item.responsable());
        }
    }

    private void asegurarDefinicion(
            String codigo,
            String nombre,
            CategoriaKpi categoria,
            String descripcion,
            String formula,
            String unidad,
            SentidoKpi sentido,
            OperadorMeta operador,
            BigDecimal meta,
            String responsable) {
        KpiDefinicion definicion = definiciones.findByProyectoIdIsNullAndCodigo(codigo)
                .orElseGet(() -> crearDefinicion(
                        codigo, nombre, categoria, descripcion, formula, unidad, sentido, operador, meta, responsable));
        asegurarVariables(definicion, formula);
    }

    private KpiDefinicion crearDefinicion(
            String codigo,
            String nombre,
            CategoriaKpi categoria,
            String descripcion,
            String formula,
            String unidad,
            SentidoKpi sentido,
            OperadorMeta operador,
            BigDecimal meta,
            String responsable) {
        KpiDefinicion definicion = new KpiDefinicion();
        definicion.proyectoId = null;
        definicion.codigo = codigo;
        definicion.nombre = nombre;
        definicion.categoria = categoria;
        definicion.descripcion = descripcion;
        definicion.formula = formula;
        definicion.unidad = unidad;
        definicion.sentido = sentido;
        definicion.operadorMeta = operador;
        definicion.meta = meta;
        definicion.metaMax = null;
        definicion.umbralVerde = BigDecimal.ONE;
        definicion.umbralAmarillo = new BigDecimal("0.8000");
        definicion.responsable = responsable;
        definicion.activo = true;
        return definiciones.saveAndFlush(definicion);
    }

    /** Crea los nombres de la fórmula que falten. No borra ni reescribe los que ya están. */
    private void asegurarVariables(KpiDefinicion definicion, String formula) {
        Set<String> presentes = new HashSet<>();
        for (KpiVariable variable : variables.findByKpiIdOrderByNombreAsc(definicion.id)) {
            presentes.add(variable.nombre);
        }
        for (String nombre : FormulaKpi.nombres(formula)) {
            if (presentes.contains(nombre)) {
                continue;
            }
            KpiVariable variable = new KpiVariable();
            variable.kpiId = definicion.id;
            variable.nombre = nombre;
            variables.save(variable);
        }
    }

    private void exigirProyecto(Long proyectoId) {
        if (proyectoId == null || !proyectos.existsById(proyectoId)) {
            throw ApiException.noEncontrado("No existe el proyecto " + proyectoId + ".");
        }
    }

    private List<KpiDefinicion> aplicables(Long proyectoId) {
        Map<String, KpiDefinicion> porCodigo = new LinkedHashMap<>();
        for (KpiDefinicion global : definiciones.findByProyectoIdIsNullAndActivoTrueOrderByCodigoAsc()) {
            porCodigo.put(global.codigo, global);
        }
        for (KpiDefinicion propia : definiciones.findByProyectoIdAndActivoTrueOrderByCodigoAsc(proyectoId)) {
            porCodigo.put(propia.codigo, propia);
        }
        return List.copyOf(porCodigo.values());
    }

    private ResultadoKpi evaluar(
            KpiDefinicion definicion, Map<String, BigDecimal> valores, java.util.Set<String> noCalculables) {
        for (String nombre : FormulaKpi.nombres(definicion.formula)) {
            if (noCalculables.contains(nombre) || !valores.containsKey(nombre) || valores.get(nombre) == null) {
                return ResultadoKpi.sinValor();
            }
        }
        BigDecimal meta = definicion.meta;
        if (meta == null) {
            BigDecimal plan = valores.get(VariablesProyecto.AVANCE_PLAN);
            if (plan == null) {
                return ResultadoKpi.sinValor();
            }
            meta = plan.multiply(new BigDecimal("100"));
        }
        DefinicionKpi efectiva = new DefinicionKpi(
                definicion.formula,
                definicion.sentido,
                definicion.operadorMeta,
                meta,
                definicion.metaMax,
                definicion.umbralVerde,
                definicion.umbralAmarillo,
                definicion.unidad);
        return evaluador.evaluar(efectiva, valores);
    }

    private static KpiEstadoResponse estado(KpiDefinicion definicion, KpiHistorico ultimo, String alerta) {
        return new KpiEstadoResponse(
                definicion.id,
                definicion.codigo,
                definicion.nombre,
                definicion.categoria.name(),
                definicion.formula,
                definicion.unidad,
                definicion.sentido.name(),
                definicion.operadorMeta.name(),
                definicion.meta,
                ultimo == null ? null : ultimo.valor,
                ultimo == null ? null : ultimo.cumplimiento,
                ultimo == null || ultimo.semaforo == null ? null : ultimo.semaforo.name(),
                ultimo == null ? null : ultimo.motivo,
                ultimo == null ? null : ultimo.calculadoEn,
                ultimo != null && ultimo.valor == null,
                alerta != null,
                alerta);
    }

    private static KpiHistoricoResponse historico(KpiHistorico fila) {
        return new KpiHistoricoResponse(
                fila.id,
                fila.kpiId,
                fila.proyectoId,
                fila.valor,
                fila.cumplimiento,
                fila.semaforo == null ? null : fila.semaforo.name(),
                fila.motivo,
                fila.calculadoEn,
                fila.valor == null);
    }
}
