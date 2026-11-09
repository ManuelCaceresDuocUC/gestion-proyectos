package cl.proyecto.gestion.kpi;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Nombres de variable que aparecen en una fórmula. */
public final class FormulaKpi {

    private static final Pattern NOMBRE = Pattern.compile("[A-Z][A-Z0-9_]*");

    private FormulaKpi() {
    }

    public static Set<String> nombres(String formula) {
        Set<String> nombres = new LinkedHashSet<>();
        if (formula == null) {
            return Set.of();
        }
        Matcher matcher = NOMBRE.matcher(formula);
        while (matcher.find()) {
            nombres.add(matcher.group());
        }
        return Set.copyOf(nombres);
    }
}
