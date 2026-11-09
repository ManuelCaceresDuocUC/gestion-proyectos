# Catálogo de variables de KPI (T-02)

Acordado entre A, B y C. Las fórmulas del seed (`db/seed/kpi-definiciones.json`) solo pueden
usar estos nombres. El paso `KPI` (orden 60, pendiente B) resuelve cada variable **del mismo
proyecto y período**; si un denominador queda 0, el KPI es «No calculable» (valor nulo, sin semáforo).

El administrador de KPI (B-12 / B-15) no inventa nombres fuera de esta lista sin actualizar
este archivo y `docs/requerimientos.md` en el mismo PR.

Semáforo (cumplimiento, tope 1): Verde ≥ 100 %, Amarillo 80–99 %, Rojo < 80 %.
MAYOR_MEJOR = valor/meta; MENOR_MEJOR = meta/valor (1 si valor = 0). Solo se redondea el
resultado final a 2 decimales.

Unidad `%`: la fórmula del seed es una razón (`numerador / denominador`, sin `* 100`).
`EvaluadorKpi` la pasa a puntos porcentuales una sola vez, antes de redondear, y esa cifra
se compara con la meta (80, 90, 15…). `días` e `índice` no se escalan. Un denominador 0
sigue siendo «No calculable» y no se escala.

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
| `ETAPAS_TOTALES` | 10 × solicitudes del proyecto (todas; B-17) |
| `CAMBIOS_ADOPTADOS` | etapa 10 `COMPLETADO` |
| `COSTO_REAL_CAMBIOS_CERRADOS` | suma `costoReal` de las cerradas |
| `PRESUPUESTO_CAMBIOS_CERRADOS` | suma `presupuestoPrevisto` de las cerradas |
| `DIAS_ATRASO_CAMBIOS` | suma de `diasAtraso` |
| `CAMBIOS_APROBADOS` | estado `APROBADO`, `EN_IMPLEMENTACION` o `CERRADO` |
| `CAMBIOS_APROBADOS_APLICADOS` | `aplicada = true` |

`CAMBIOS_APROBADOS` (B-17): cuenta el estado, no `aprobadaEn`. Entran `APROBADO`,
`EN_IMPLEMENTACION` y `CERRADO`. Quedan fuera `ABIERTO`, `EN_EVALUACION` y `RECHAZADO`.
`RECHAZADO` tampoco es `CAMBIOS_CERRADOS`; solo entra en el atraso (si no hay fin real, el atraso es 0,
igual que un `CERRADO` sin fin real).

`ETAPAS_TOTALES` (B-17): 10 × `CAMBIOS_TOTAL`. «Seguidos» son todas las solicitudes del proyecto,
hayan generado o no sus filas de etapa. Sin solicitudes el denominador es 0 y KC-06 queda
«No calculable».

`CAMBIOS_CON_CORRECTIVA`: etapa 9 en un estado distinto de `PENDIENTE` y `NO_APLICA`, o
`accionCorrectiva` con texto (un blanco no cuenta). La solicitud suma una sola vez.

KC-11…KC-13 siguen siendo los tres adicionales de `db/seed/kpi-definiciones.json`. El issue
#88 no tiene respuesta del profesor; no hay ADR hasta consultarlo. El seed **no carga**
estas definiciones hasta que exista `KpiService`.
