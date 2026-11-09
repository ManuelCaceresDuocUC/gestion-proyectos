# API de tableros (esqueleto T-02 / B-21, C-12, C-13)

Pendiente de implementación. Los tableros **solo leen**. No recalculan.
Contenido funcional: especificación C-02 en `docs/requerimientos.md`.
El JSON de respuesta lo fijan B-21 y C-12 en sus PR; aquí van rutas y filtros.

JWT. `404` si el proyecto no existe.

## Rutas previstas

| Método | Ruta | Dueño |
|---|---|---|
| GET | `/tablero/proyecto/{proyectoId}` | B-21 |
| GET | `/tablero/cambio/{proyectoId}` | C-12 |

Query (C-13 / guía):

| Tablero | Filtros |
|---|---|
| Proyecto | `etapaId`, `desde`, `hasta`, `responsableId` (el proyecto ya va en la URL) |
| Cambio | `estado`, `prioridad`, `riesgo`, `responsableId` |

Un cambio de filtro reconsulta; el cliente no agrega totales que el servidor ya mandó.
Tras un recálculo o un cambio de solicitud, el front vuelve a pedir estos GET (C-13).

## Qué debe traer (sin esquema cerrado)

**Proyecto:** avance real vs plan; hitos cumplidos / próximos / vencidos; costos plan, real y
presupuesto; uso de recursos y sobreasignación; actividades atrasadas; KPI de proyecto/hitos
con semáforo. Los KPI salen del histórico (paso 60), no se evalúan aquí.

**Cambio:** conteos por estado V21; en evaluación / implementación; cerrados; rechazados;
atrasados; avance promedio de etapas; riesgos altos/críticos; costos de cerradas; adopción
(etapa 10); efectividad (`resultadoEvaluacion`); alertas (atraso, bloqueo, KPI en rojo).
KPI de cambio: B-17.

Layout compartido: `frontend/src/features/tableros`. Dos pantallas distintas.
