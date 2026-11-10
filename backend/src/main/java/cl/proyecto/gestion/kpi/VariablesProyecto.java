package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Variables de KP-01…KP-08. Dominio puro: no lee la base.
 * Los porcentajes de avance entran en puntos (0–100), como {@code proyecto.avance},
 * y salen en razón 0–1 para que {@code EvaluadorKpi} aplique el × 100.
 */
public final class VariablesProyecto {

    public static final String AVANCE_REAL = "AVANCE_REAL";
    public static final String AVANCE_PLAN = "AVANCE_PLAN";
    public static final String VALOR_PLANIFICADO = "VALOR_PLANIFICADO";
    public static final String VALOR_GANADO = "VALOR_GANADO";
    public static final String COSTO_REAL = "COSTO_REAL";
    public static final String COSTO_PLAN_A_LA_FECHA = "COSTO_PLAN_A_LA_FECHA";
    public static final String DIAS_ATRASO_PROYECTO = "DIAS_ATRASO_PROYECTO";
    public static final String ACTIVIDADES_TOTAL = "ACTIVIDADES_TOTAL";
    public static final String ACTIVIDADES_ATRASADAS = "ACTIVIDADES_ATRASADAS";
    public static final String HITOS_VENCIDOS = "HITOS_VENCIDOS";
    public static final String HITOS_CUMPLIDOS_A_TIEMPO = "HITOS_CUMPLIDOS_A_TIEMPO";
    public static final String PROFESIONALES_TOTAL = "PROFESIONALES_TOTAL";
    public static final String PROFESIONALES_SOBREASIGNADOS = "PROFESIONALES_SOBREASIGNADOS";

    /** Nombre del tipo de recurso que cuenta como profesional (seed C-07). */
    public static final String TIPO_PROFESIONAL = "Profesional";

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int ESCALA_RAZON = 8;

    private VariablesProyecto() {
    }

    public static Set<String> nombres() {
        return Set.of(
                AVANCE_REAL, AVANCE_PLAN, VALOR_PLANIFICADO, VALOR_GANADO, COSTO_REAL,
                COSTO_PLAN_A_LA_FECHA, DIAS_ATRASO_PROYECTO, ACTIVIDADES_TOTAL, ACTIVIDADES_ATRASADAS,
                HITOS_VENCIDOS, HITOS_CUMPLIDOS_A_TIEMPO, PROFESIONALES_TOTAL, PROFESIONALES_SOBREASIGNADOS);
    }

    /**
     * La misma regla que {@code ACTIVIDADES_ATRASADAS}: sin hijos, fin plan anterior a hoy
     * y avance menor que 100. Un avance nulo cuenta como 0.
     */
    public static boolean hojaAtrasada(
            boolean tieneHijos, LocalDate finPlan, BigDecimal avancePuntos, LocalDate hoy) {
        if (tieneHijos || finPlan == null || hoy == null || !finPlan.isBefore(hoy)) {
            return false;
        }
        return puntos(avancePuntos).compareTo(CIEN) < 0;
    }

    public static Medicion calcular(Datos datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del proyecto son obligatorios.");
        }
        if (datos.tareas() == null || datos.hitos() == null) {
            throw new IllegalArgumentException("Las tareas y los hitos son obligatorios.");
        }
        if (datos.calendario() == null || datos.hoy() == null) {
            throw new IllegalArgumentException("El calendario y la fecha de referencia son obligatorios.");
        }
        if (datos.profesionales() < 0 || datos.profesionalesSobreasignados() < 0) {
            throw new IllegalArgumentException("Los conteos de profesionales no pueden ser negativos.");
        }

        Map<Long, List<Nodo>> hijos = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        for (Nodo nodo : datos.tareas()) {
            ids.add(nodo.id());
        }
        List<Nodo> raices = new ArrayList<>();
        for (Nodo nodo : datos.tareas()) {
            if (nodo.parentId() != null && ids.contains(nodo.parentId())) {
                hijos.computeIfAbsent(nodo.parentId(), k -> new ArrayList<>()).add(nodo);
            } else {
                raices.add(nodo);
            }
        }

        Map<Long, BigDecimal> planPuntos = new HashMap<>();
        for (Nodo raiz : raices) {
            planificar(raiz, hijos, planPuntos, datos.calendario(), datos.hoy());
        }
        BigDecimal avancePlanPuntos = promedio(raices, planPuntos);

        BigDecimal valorPlanificado = BigDecimal.ZERO;
        BigDecimal valorGanado = BigDecimal.ZERO;
        int actividades = 0;
        int atrasadas = 0;
        for (Nodo nodo : datos.tareas()) {
            if (hijos.containsKey(nodo.id())) {
                continue;
            }
            actividades++;
            BigDecimal puntosHoja = planPuntos.getOrDefault(nodo.id(), CERO);
            BigDecimal costo = nodo.costoPlan() == null ? BigDecimal.ZERO : nodo.costoPlan();
            valorPlanificado = valorPlanificado.add(costo.multiply(razon(puntosHoja)));
            valorGanado = valorGanado.add(costo.multiply(razon(nodo.avancePuntos())));
            if (hojaAtrasada(false, nodo.finPlan(), nodo.avancePuntos(), datos.hoy())) {
                atrasadas++;
            }
        }

        int vencidos = 0;
        int aTiempo = 0;
        for (HitoMedido hito : datos.hitos()) {
            if (hito.fechaPlan() == null || !hito.fechaPlan().isBefore(datos.hoy())) {
                continue;
            }
            vencidos++;
            if (hito.cumplido()
                    && hito.fechaReal() != null
                    && !hito.fechaReal().isAfter(hito.fechaPlan())) {
                aTiempo++;
            }
        }

        Map<String, BigDecimal> valores = new LinkedHashMap<>();
        valores.put(AVANCE_REAL, razon(datos.avanceRealPuntos()));
        valores.put(AVANCE_PLAN, razon(avancePlanPuntos));
        valores.put(VALOR_PLANIFICADO, valorPlanificado);
        valores.put(VALOR_GANADO, valorGanado);
        valores.put(COSTO_REAL, datos.costoReal() == null ? BigDecimal.ZERO : datos.costoReal());
        valores.put(COSTO_PLAN_A_LA_FECHA, valorPlanificado);
        Set<String> noCalculables = new HashSet<>();
        Integer dias = diasAtraso(datos);
        if (dias == null) {
            noCalculables.add(DIAS_ATRASO_PROYECTO);
        } else {
            valores.put(DIAS_ATRASO_PROYECTO, BigDecimal.valueOf(dias));
        }
        valores.put(ACTIVIDADES_TOTAL, BigDecimal.valueOf(actividades));
        valores.put(ACTIVIDADES_ATRASADAS, BigDecimal.valueOf(atrasadas));
        valores.put(HITOS_VENCIDOS, BigDecimal.valueOf(vencidos));
        valores.put(HITOS_CUMPLIDOS_A_TIEMPO, BigDecimal.valueOf(aTiempo));
        valores.put(PROFESIONALES_TOTAL, BigDecimal.valueOf(datos.profesionales()));
        valores.put(PROFESIONALES_SOBREASIGNADOS, BigDecimal.valueOf(datos.profesionalesSobreasignados()));
        return new Medicion(Map.copyOf(valores), Set.copyOf(noCalculables));
    }

    /**
     * Sin línea base: 0. Con línea base y sin fin plan: no se puede medir (null).
     * Con ambas fechas: días hábiles de la base al plan, como {@code ComparadorPlanes}, mínimo 0.
     */
    static Integer diasAtraso(Datos datos) {
        if (datos.fechaFinBase() == null) {
            return 0;
        }
        if (datos.fechaFinPlan() == null) {
            return null;
        }
        return Math.max(0, diferencia(datos.calendario(), datos.fechaFinBase(), datos.fechaFinPlan()));
    }

    /** Positivo si {@code hasta} es posterior. Misma cuenta que {@code ComparadorPlanes.diferencia}. */
    static int diferencia(Calendario cal, LocalDate desde, LocalDate hasta) {
        if (hasta.isAfter(desde)) {
            return cal.diasHabilesEntre(desde.plusDays(1), hasta);
        }
        if (hasta.isBefore(desde)) {
            return -cal.diasHabilesEntre(hasta.plusDays(1), desde);
        }
        return 0;
    }

    private static void planificar(
            Nodo nodo,
            Map<Long, List<Nodo>> hijos,
            Map<Long, BigDecimal> planPuntos,
            Calendario calendario,
            LocalDate hoy) {
        List<Nodo> hs = hijos.getOrDefault(nodo.id(), List.of());
        if (hs.isEmpty()) {
            planPuntos.put(nodo.id(), avancePlanHoja(nodo, calendario, hoy));
            return;
        }
        for (Nodo hijo : hs) {
            planificar(hijo, hijos, planPuntos, calendario, hoy);
        }
        planPuntos.put(nodo.id(), promedio(hs, planPuntos));
    }

    /** Promedio ponderado por duración; si todos los pesos son 0, promedio simple. */
    static BigDecimal promedio(List<Nodo> nodos, Map<Long, BigDecimal> planPuntos) {
        if (nodos.isEmpty()) {
            return CERO;
        }
        BigDecimal suma = BigDecimal.ZERO;
        BigDecimal pesos = BigDecimal.ZERO;
        for (Nodo nodo : nodos) {
            BigDecimal peso = BigDecimal.valueOf(nodo.duracionDias() == null ? 0 : nodo.duracionDias());
            BigDecimal valor = planPuntos.getOrDefault(nodo.id(), CERO);
            suma = suma.add(valor.multiply(peso));
            pesos = pesos.add(peso);
        }
        if (pesos.signum() == 0) {
            BigDecimal total = nodos.stream()
                    .map(n -> planPuntos.getOrDefault(n.id(), CERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.divide(BigDecimal.valueOf(nodos.size()), 2, RoundingMode.HALF_UP);
        }
        return suma.divide(pesos, 2, RoundingMode.HALF_UP);
    }

    static BigDecimal avancePlanHoja(Nodo nodo, Calendario calendario, LocalDate hoy) {
        if (nodo.finPlan() != null && nodo.finPlan().isBefore(hoy)) {
            return CIEN.setScale(2, RoundingMode.HALF_UP);
        }
        int duracion = nodo.duracionDias() == null ? 0 : nodo.duracionDias();
        if (duracion <= 0 || nodo.inicioPlan() == null || nodo.finPlan() == null) {
            if (nodo.finPlan() != null && !nodo.finPlan().isAfter(hoy)) {
                return CIEN.setScale(2, RoundingMode.HALF_UP);
            }
            return CERO;
        }
        if (hoy.isBefore(nodo.inicioPlan())) {
            return CERO;
        }
        LocalDate hasta = hoy.isAfter(nodo.finPlan()) ? nodo.finPlan() : hoy;
        int transcurridos = calendario.diasHabilesEntre(nodo.inicioPlan(), hasta);
        BigDecimal ratio = BigDecimal.valueOf(transcurridos)
                .divide(BigDecimal.valueOf(duracion), ESCALA_RAZON, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }
        return ratio.multiply(CIEN).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal razon(BigDecimal puntos) {
        return puntos(puntos).divide(CIEN, ESCALA_RAZON, RoundingMode.HALF_UP);
    }

    private static BigDecimal puntos(BigDecimal puntos) {
        return puntos == null ? BigDecimal.ZERO : puntos;
    }

    public record Medicion(Map<String, BigDecimal> valores, Set<String> noCalculables) {
    }

    public record Datos(
            BigDecimal avanceRealPuntos,
            LocalDate fechaFinBase,
            LocalDate fechaFinPlan,
            List<Nodo> tareas,
            List<HitoMedido> hitos,
            int profesionales,
            int profesionalesSobreasignados,
            Calendario calendario,
            LocalDate hoy,
            BigDecimal costoReal) {
    }

    public record Nodo(
            long id,
            Long parentId,
            Integer duracionDias,
            BigDecimal avancePuntos,
            BigDecimal costoPlan,
            LocalDate inicioPlan,
            LocalDate finPlan) {
    }

    public record HitoMedido(LocalDate fechaPlan, LocalDate fechaReal, boolean cumplido) {
    }
}
