package cl.proyecto.gestion.kpi;

import static org.assertj.core.api.Assertions.assertThat;

import cl.proyecto.gestion.planificacion.motor.Calendario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VariablesProyectoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);
    private static final Calendario CALENDARIO = new Calendario(Set.of());

    @Test
    void normalizaElAvanceRealYPonderaComoLaConsolidacion() {
        VariablesProyecto.Nodo etapaA = nodo(1, null, 10, "0", "0", null, null);
        VariablesProyecto.Nodo hojaA = nodo(2, 1L, 1, "40", "100",
                LocalDate.of(2000, 1, 3), LocalDate.of(2000, 1, 7));
        VariablesProyecto.Nodo etapaB = nodo(3, null, 30, "0", "0", null, null);
        VariablesProyecto.Nodo hojaB = nodo(4, 3L, 1, "0", "0",
                LocalDate.of(2099, 1, 4), LocalDate.of(2099, 1, 8));

        VariablesProyecto.Medicion medicion = VariablesProyecto.calcular(datos(
                new BigDecimal("50.00"),
                null,
                LocalDate.of(2000, 1, 7),
                List.of(etapaA, hojaA, etapaB, hojaB),
                List.of(),
                0,
                0,
                new BigDecimal("10")));

        assertThat(medicion.valores().get(VariablesProyecto.AVANCE_REAL)).isEqualByComparingTo("0.50");
        assertThat(medicion.valores().get(VariablesProyecto.AVANCE_PLAN)).isEqualByComparingTo("0.25");
        assertThat(medicion.valores().get(VariablesProyecto.VALOR_GANADO)).isEqualByComparingTo("40");
        assertThat(medicion.valores().get(VariablesProyecto.VALOR_PLANIFICADO)).isEqualByComparingTo("100");
        assertThat(medicion.valores().get(VariablesProyecto.COSTO_REAL)).isEqualByComparingTo("10");
        assertThat(medicion.valores().get(VariablesProyecto.DIAS_ATRASO_PROYECTO)).isEqualByComparingTo("0");
    }

    @Test
    void conPesosCeroUsaPromedioSimple() {
        VariablesProyecto.Nodo padre = nodo(1, null, 0, "0", "0", null, null);
        VariablesProyecto.Nodo hecha = nodo(2, 1L, 0, "0", "0",
                LocalDate.of(2000, 1, 3), LocalDate.of(2000, 1, 3));
        VariablesProyecto.Nodo futura = nodo(3, 1L, 0, "0", "0",
                LocalDate.of(2099, 1, 4), LocalDate.of(2099, 1, 4));

        VariablesProyecto.Medicion medicion = VariablesProyecto.calcular(datos(
                BigDecimal.ZERO, null, null,
                List.of(padre, hecha, futura), List.of(), 0, 0, BigDecimal.ZERO));

        assertThat(medicion.valores().get(VariablesProyecto.AVANCE_PLAN)).isEqualByComparingTo("0.50");
    }

    @Test
    void cuentaHojasPorAusenciaDeHijosYHitosVencidos() {
        VariablesProyecto.Nodo etapa = nodo(1, null, 5, "0", "0", null, null);
        VariablesProyecto.Nodo subactividadConHijo = nodo(2, 1L, 5, "100", "0",
                LocalDate.of(2000, 1, 3), LocalDate.of(2000, 1, 7));
        VariablesProyecto.Nodo hojaAtrasada = nodo(3, 2L, 5, "40", "0",
                LocalDate.of(2000, 1, 3), LocalDate.of(2000, 1, 7));
        VariablesProyecto.Nodo hojaAlDia = nodo(4, 1L, 5, "100", "0",
                LocalDate.of(2000, 1, 3), LocalDate.of(2000, 1, 7));

        VariablesProyecto.Medicion medicion = VariablesProyecto.calcular(datos(
                BigDecimal.ZERO, null, null,
                List.of(etapa, subactividadConHijo, hojaAtrasada, hojaAlDia),
                List.of(
                        new VariablesProyecto.HitoMedido(LocalDate.of(2000, 1, 4), LocalDate.of(2000, 1, 4), true),
                        new VariablesProyecto.HitoMedido(LocalDate.of(2000, 1, 5), null, false),
                        new VariablesProyecto.HitoMedido(LocalDate.of(2000, 1, 6), LocalDate.of(2000, 1, 8), true),
                        new VariablesProyecto.HitoMedido(LocalDate.of(2099, 1, 1), null, false)),
                2,
                1,
                BigDecimal.ZERO));

        assertThat(medicion.valores().get(VariablesProyecto.ACTIVIDADES_TOTAL)).isEqualByComparingTo("2");
        assertThat(medicion.valores().get(VariablesProyecto.ACTIVIDADES_ATRASADAS)).isEqualByComparingTo("1");
        assertThat(medicion.valores().get(VariablesProyecto.HITOS_VENCIDOS)).isEqualByComparingTo("3");
        assertThat(medicion.valores().get(VariablesProyecto.HITOS_CUMPLIDOS_A_TIEMPO)).isEqualByComparingTo("1");
        assertThat(medicion.valores().get(VariablesProyecto.PROFESIONALES_TOTAL)).isEqualByComparingTo("2");
        assertThat(medicion.valores().get(VariablesProyecto.PROFESIONALES_SOBREASIGNADOS)).isEqualByComparingTo("1");
    }

    @Test
    void atrasoSinLineaBaseEsCeroYSinFinPlanNoEsCalculable() {
        VariablesProyecto.Medicion sinBase = VariablesProyecto.calcular(datos(
                BigDecimal.ZERO, null, LocalDate.of(2000, 1, 7),
                List.of(), List.of(), 0, 0, BigDecimal.ZERO));
        assertThat(sinBase.valores().get(VariablesProyecto.DIAS_ATRASO_PROYECTO)).isEqualByComparingTo("0");
        assertThat(sinBase.noCalculables()).isEmpty();

        VariablesProyecto.Medicion sinFin = VariablesProyecto.calcular(datos(
                BigDecimal.ZERO, LocalDate.of(2000, 1, 6), null,
                List.of(), List.of(), 0, 0, BigDecimal.ZERO));
        assertThat(sinFin.valores()).doesNotContainKey(VariablesProyecto.DIAS_ATRASO_PROYECTO);
        assertThat(sinFin.noCalculables()).contains(VariablesProyecto.DIAS_ATRASO_PROYECTO);

        VariablesProyecto.Medicion adelantado = VariablesProyecto.calcular(datos(
                BigDecimal.ZERO, LocalDate.of(2000, 1, 7), LocalDate.of(2000, 1, 6),
                List.of(), List.of(), 0, 0, BigDecimal.ZERO));
        assertThat(adelantado.valores().get(VariablesProyecto.DIAS_ATRASO_PROYECTO)).isEqualByComparingTo("0");

        VariablesProyecto.Medicion unDia = VariablesProyecto.calcular(datos(
                BigDecimal.ZERO, LocalDate.of(2000, 1, 6), LocalDate.of(2000, 1, 7),
                List.of(), List.of(), 0, 0, BigDecimal.ZERO));
        assertThat(unDia.valores().get(VariablesProyecto.DIAS_ATRASO_PROYECTO)).isEqualByComparingTo("1");
    }

    private static VariablesProyecto.Datos datos(
            BigDecimal avance,
            LocalDate finBase,
            LocalDate finPlan,
            List<VariablesProyecto.Nodo> tareas,
            List<VariablesProyecto.HitoMedido> hitos,
            int profesionales,
            int sobreasignados,
            BigDecimal costoReal) {
        return new VariablesProyecto.Datos(
                avance, finBase, finPlan, tareas, hitos, profesionales, sobreasignados,
                CALENDARIO, HOY, costoReal);
    }

    private static VariablesProyecto.Nodo nodo(
            long id, Long parentId, int duracion, String avance, String costo, LocalDate inicio, LocalDate fin) {
        return new VariablesProyecto.Nodo(
                id, parentId, duracion, new BigDecimal(avance), new BigDecimal(costo), inicio, fin);
    }
}
