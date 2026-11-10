# C-14 · Recálculo 3: aplicar una solicitud TIEMPO

Corresponde al tercer recálculo de la demo (RF-INT-04 / CP-03). Automatizado en
`RecalculoSolicitudE2ETest`. Aprobar y aplicar ya existían (C-08, C-09); este issue
verifica el efecto en plan, costos, KPI y tableros por HTTP, como la interfaz.

El issue #110 decía «aprobar SC-001 y que se muevan fechas». En código **aprobar no
mueve el plan**. El Gantt cambia al **aplicar** una solicitud **TIEMPO**. El seed no
trae TIEMPO (`SC-01`…`SC-05` son Tecnología u Organización): el E2E crea una. En la
demo se edita `SC-01` a tipo Tiempo (guion).

## Datos de entrada

Proyecto con inicio lunes 2-nov-2026, una etapa, dos hojas y una asignación (15 h × 100):

| Código | Tarea | Duración | Dependencia |
|---|---|---|---|
| 1.1 | A | 5 | — |
| 1.2 | B | 2 | FS desde A |

Se fija la línea base (v1). Alta TIEMPO sobre A, `impactoDias` = 3. Flujo HTTP:

1. PUT a `EN_EVALUACION`
2. `POST /api/cambios/{id}/aprobar` (solo ADMIN)
3. `POST /api/cambios/{id}/aplicar`

## Resultado esperado

| Paso | Plan | Línea base | Costos | KPI / tableros |
|---|---|---|---|---|
| Evaluar / aprobar | A sigue en 5 d; desviación 0 | `finBase` igual | plan igual | sin snapshot nuevo |
| Aplicar TIEMPO | A → 8 d; B (FS) +3 d hábiles | `finBase` igual | plan igual (misma cantidad) | +21 filas en `kpi_historico`; GET KPI, tablero proyecto (KP) y tablero cambio (KC, `EN_IMPLEMENTACION`) |

- `USUARIO_PROYECTO` recibe 403 `PROHIBIDO` al aprobar; A no cambia.
- Aplicar un no-TIEMPO no llama al recálculo de fechas (C-09).

## Resultado obtenido

| Prueba | Fecha | Resultado | Responsable |
|---|---|---|---|
| `RecalculoSolicitudE2ETest` (HTTP) | 10 nov 2026 | exit 0 (5433) | C |
| Manual `/cambios` + Gantt + `/tableros` (capturas) | — | Pendiente T-07 | C |
