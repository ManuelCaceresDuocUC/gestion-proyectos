package cl.proyecto.gestion.kpi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Evalúa una fórmula de KPI con exp4j. Dominio puro: no usa Spring ni la base.
 * Las variables entran sin redondear. Si la unidad es {@code %}, la razón se multiplica por 100
 * una sola vez (la fórmula no debe traer ese factor). Solo el valor final queda a 2 decimales.
 * Un denominador cero (o un resultado no finito) devuelve «No calculable», nunca error ni infinito.
 */
public final class EvaluadorKpi {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int ESCALA_VALOR = 2;
    private static final int ESCALA_CUMPLIMIENTO = 4;
    private static final int ESCALA_DIVISION = 8;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    public ResultadoKpi evaluar(DefinicionKpi definicion, Map<String, BigDecimal> variables) {
        if (definicion == null) {
            throw new IllegalArgumentException("La definición del KPI es obligatoria.");
        }
        if (variables == null) {
            throw new IllegalArgumentException("Las variables del KPI son obligatorias.");
        }
        Double crudo = evaluarFormula(definicion.formula(), variables);
        if (crudo == null) {
            return ResultadoKpi.sinValor();
        }
        BigDecimal exacto = BigDecimal.valueOf(crudo);
        if (definicion.esPorcentaje()) {
            exacto = exacto.multiply(CIEN);
        }
        BigDecimal valor = exacto.setScale(ESCALA_VALOR, REDONDEO);
        BigDecimal cumplimiento = cumplimiento(definicion, exacto);
        SemaforoKpi semaforo = cumplimiento == null ? null : semaforo(cumplimiento, definicion);
        return new ResultadoKpi(valor, cumplimiento, semaforo, cumpleMeta(definicion, exacto));
    }

    private Double evaluarFormula(String formula, Map<String, BigDecimal> variables) {
        ExpressionBuilder builder = new ExpressionBuilder(formula.trim());
        if (!variables.isEmpty()) {
            builder.variables(variables.keySet());
        }
        Expression expression = builder.build();
        for (var entrada : variables.entrySet()) {
            if (entrada.getKey() == null || entrada.getKey().isBlank()) {
                throw new IllegalArgumentException("El nombre de la variable es obligatorio.");
            }
            if (entrada.getValue() == null) {
                throw new IllegalArgumentException("La variable " + entrada.getKey() + " no tiene valor.");
            }
            expression.setVariable(entrada.getKey(), entrada.getValue().doubleValue());
        }
        try {
            double resultado = expression.evaluate();
            return Double.isFinite(resultado) ? resultado : null;
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private BigDecimal cumplimiento(DefinicionKpi definicion, BigDecimal valor) {
        if (definicion.sentido() == SentidoKpi.MENOR_MEJOR && valor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE.setScale(ESCALA_CUMPLIMIENTO, REDONDEO);
        }
        if (definicion.sentido() == SentidoKpi.MAYOR_MEJOR && definicion.meta().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal ratio = switch (definicion.sentido()) {
            case MAYOR_MEJOR -> valor.divide(definicion.meta(), ESCALA_DIVISION, REDONDEO);
            case MENOR_MEJOR -> definicion.meta().divide(valor, ESCALA_DIVISION, REDONDEO);
        };
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }
        return ratio.setScale(ESCALA_CUMPLIMIENTO, REDONDEO);
    }

    private SemaforoKpi semaforo(BigDecimal cumplimiento, DefinicionKpi definicion) {
        if (cumplimiento.compareTo(definicion.umbralVerde()) >= 0) {
            return SemaforoKpi.VERDE;
        }
        if (cumplimiento.compareTo(definicion.umbralAmarillo()) >= 0) {
            return SemaforoKpi.AMARILLO;
        }
        return SemaforoKpi.ROJO;
    }

    private boolean cumpleMeta(DefinicionKpi definicion, BigDecimal valor) {
        return switch (definicion.operador()) {
            case MAYOR_IGUAL -> valor.compareTo(definicion.meta()) >= 0;
            case MENOR_IGUAL -> valor.compareTo(definicion.meta()) <= 0;
            case MAYOR -> valor.compareTo(definicion.meta()) > 0;
            case MENOR -> valor.compareTo(definicion.meta()) < 0;
            case ENTRE -> valor.compareTo(definicion.meta()) >= 0
                    && valor.compareTo(definicion.metaMax()) <= 0;
        };
    }
}
