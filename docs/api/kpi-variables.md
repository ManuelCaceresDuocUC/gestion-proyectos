# Catálogo de variables de KPI (T-02)

Acordado entre A, B y C. Las fórmulas del seed (`db/seed/kpi-definiciones.json`) solo pueden
usar estos nombres. El paso `KPI` (orden 60, pendiente B) resuelve cada variable **del mismo
proyecto y período**; si un denominador queda 0, el KPI es «No calculable» (valor nulo, sin semáforo).

El administrador de KPI (B-12 / B-15) no inventa nombres fuera de esta lista sin actualizar
este archivo y `docs/requerimientos.md` en el mismo PR.

Semáforo (cumplimiento, tope 1): Verde ≥ 100 %, Amarillo 80–99 %, Rojo < 80 %.
MAYOR_MEJOR = valor/meta; MENOR_MEJOR = meta/valor (1 si valor = 0). Solo se redondea el
resultado final a 2 decimales.

## Proyecto e hitos (KP-01 … KP-08)

| Variable | Significado | Origen |
|---|---|---|
| `AVANCE_REAL` | % de avance físico consolidado de la EDT | `proyecto.avance` / consolidación |
| `AVANCE_PLAN` | % de avance planificado a la fecha | plan vs hoy |
| `VALOR_PLANIFICADO` | PV: costo plan × % planificado | costos + avance plan |
| `VALOR_GANADO` | EV: costo plan × % real | costos + avance real |
| `COSTO_REAL` | AC acumulado | `proyecto.costoReal` / hojas |
| `COSTO_PLAN_A_LA_FECHA` | costo plan acumulado a la fecha | costos + calendario |
| `DIAS_ATRASO_PROYECTO` | fin plan actual − fin de línea base | comparación A-14 |
| `ACTIVIDADES_TOTAL` | tareas hoja | EDT |
| `ACTIVIDADES_ATRASADAS` | hojas vencidas con avance < 100 % | plan vs hoy |
| `HITOS_VENCIDOS` | hitos con fecha vencida | módulo hito (B-11) |
| `HITOS_CUMPLIDOS_A_TIEMPO` | hitos cumplidos en plazo | módulo hito |
| `PROFESIONALES_TOTAL` | profesionales del proyecto | recursos |
| `PROFESIONALES_SOBREASIGNADOS` | con alerta de carga | paso 40 |

## Gestión del cambio (KC-01 … KC-13)

Estados V21. «Cerrado» = `CERRADO`. «En plazo» usa `diasAtraso` = 0.
«Adoptado» = etapa 10 `COMPLETADO`. «Con correctiva» = etapa 9 no `PENDIENTE`/`NO_APLICA`
o `accionCorrectiva` informado.

| Variable | Significado |
|---|---|
| `CAMBIOS_TOTAL` | solicitudes del proyecto |
| `CAMBIOS_CERRADOS` | estado `CERRADO` |
| `CAMBIOS_CERRADOS_EN_PLAZO` | cerrados con `diasAtraso` 0 |
| `CAMBIOS_RIESGO_ALTO` | riesgo `ALTO` o `CRITICO` |
| `CAMBIOS_PRIORIDAD_ALTA` | prioridad `ALTA` o `CRITICA` |
| `CAMBIOS_CON_ATRASO` | `diasAtraso` > 0 |
| `CAMBIOS_CON_BLOQUEO` | al menos una etapa `BLOQUEADO` |
| `CAMBIOS_CON_CORRECTIVA` | ver arriba |
| `CAMBIOS_CERRADOS_CON_SOBRECOSTO` | cerrados con costo real > presupuesto previsto |
| `CAMBIOS_CERRADOS_CON_PRESUPUESTO` | cerrados que tienen presupuesto previsto |
| `ETAPAS_COMPLETADAS` | etapas `COMPLETADO` (todas las solicitudes) |
| `ETAPAS_TOTALES` | 10 × solicitudes seguidas |
| `CAMBIOS_ADOPTADOS` | etapa 10 `COMPLETADO` |
| `COSTO_REAL_CAMBIOS_CERRADOS` | suma `costoReal` de las cerradas |
| `PRESUPUESTO_CAMBIOS_CERRADOS` | suma `presupuestoPrevisto` de las cerradas |
| `DIAS_ATRASO_CAMBIOS` | suma de `diasAtraso` |
| `CAMBIOS_APROBADOS` | llegaron a `APROBADO` (incluye las ya aplicadas / en implementación / cerradas si se cuenta el hecho) |
| `CAMBIOS_APROBADOS_APLICADOS` | `aplicada = true` |

`CAMBIOS_APROBADOS`: conteo de solicitudes con `aprobadaEn` no nulo (o estado ≥ `APROBADO`
en el grafo, excluyendo `RECHAZADO`). B-17 lo precisa al implementar el paso.

Las fórmulas concretas están en el JSON del seed (KP-01…08 y KC-01…13). El seed **no las
carga** hasta que exista `KpiService`.
