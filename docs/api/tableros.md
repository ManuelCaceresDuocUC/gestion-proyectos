# API de tableros (T-02 / B-21, C-12, C-13)

Los tableros **solo leen**. No llaman a `RecalculoService` ni evalúan fórmulas de KPI.
Contenido funcional: especificación C-02 en `docs/requerimientos.md`.

JWT. `404 NO_ENCONTRADO` si el proyecto no existe.

## Rutas

| Método | Ruta | Dueño | Estado |
|---|---|---|---|
| GET | `/tablero/proyecto/{proyectoId}` | B-21 | este contrato |
| GET | `/tablero/cambio/{proyectoId}` | C-12 | sin controlador; el JSON lo fija C-12 |

Query (C-13 / guía). **B-21 no los lee**: si vienen en el GET de proyecto, la respuesta no cambia.

| Tablero | Filtros |
|---|---|
| Proyecto | `etapaId`, `desde`, `hasta`, `responsableId` (el proyecto ya va en la URL) |
| Cambio | `estado`, `prioridad`, `riesgo`, `responsableId` |

Un cambio de filtro reconsulta; el cliente no agrega totales que el servidor ya mandó.
Tras un recálculo o un cambio de solicitud, el front vuelve a pedir estos GET (C-13).

Layout compartido: `frontend/src/features/tableros`. Dos pantallas distintas.
La de proyecto es B-21. La de cambio y los filtros son C-12 y C-13.

## GET `/tablero/proyecto/{proyectoId}`

Una sola fecha `hoy` (el día del servidor) clasifica hitos y actividades atrasadas y entra
en la medición de avance planificado. No se persiste nada.

| Sección | Origen | Qué no hace este GET |
|---|---|---|
| `avance.real` | `proyecto.avance` (puntos 0–100, ya consolidado) | no promedia la EDT |
| `avance.planificado` | `AVANCE_PLAN` de `VariablesProyecto` × 100, misma medición del paso 60, en puntos 0–100 | no copia la fórmula |
| `hitos` | filas de `hito`, clasificadas abajo | no corre el paso 50 |
| `costos` | `costo_plan`, `costo_real` y `presupuesto` del proyecto | no suma tareas |
| `recursos` | conteos de esa misma medición; `alertas` = filas de `sobreasignacion_alerta` | no corre el paso 40 |
| `actividadesAtrasadas` | hojas según `VariablesProyecto.hojaAtrasada` | no cuenta etapas ni actividades con hijos |
| `kpi` | último snapshot de `KpiService.listar`, solo categorías `PROYECTO` e `HITOS` | no llama a `registrarSnapshot` ni al evaluador; no incluye `CAMBIO` |

`recursos.profesionales` y `recursos.sobreasignados` son personas (tipo `Profesional`), no filas.
Salen de la misma medición que `PROFESIONALES_TOTAL` y `PROFESIONALES_SOBREASIGNADOS`.
`alertas` es la lista ya persistida (una fila por profesional y fecha): puede haber más filas que personas.

Hitos, sin solaparse:

| Grupo | Regla |
|---|---|
| `cumplidos` | `estado = CUMPLIDO` |
| `vencidos` | estado distinto de `CUMPLIDO` y `fechaPlan` anterior a `hoy` |
| `proximos` | el resto, incluida `fechaPlan` nula |

Actividades atrasadas: la tarea no tiene hijos, `finPlan` es anterior a `hoy` y `avance` es menor que 100.
Un avance nulo cuenta como 0. Una hoja al 100 % no entra, aunque su fin ya pasó.

```json
{
  "proyectoId": 1,
  "codigo": "EEC-2026",
  "nombre": "Proyecto",
  "avance": { "real": 33.00, "planificado": 100.00 },
  "hitos": {
    "cumplidos": [
      {
        "id": 1,
        "codigo": "H1",
        "logro": "Entrega aceptada",
        "fechaPlan": "2026-10-01",
        "fechaReal": "2026-10-01",
        "estado": "CUMPLIDO"
      }
    ],
    "proximos": [],
    "vencidos": []
  },
  "costos": { "plan": 800.00, "real": 500.00, "presupuesto": 1500.00, "moneda": "CLP" },
  "recursos": {
    "profesionales": 5,
    "sobreasignados": 1,
    "alertas": [
      {
        "id": 1,
        "recursoId": 3,
        "profesional": "Ana Pérez",
        "fecha": "2026-11-10",
        "cargaHoras": 12.0000,
        "capacidadHorasDia": 8.0,
        "excesoHoras": 4.0000,
        "tareas": [{ "id": 15, "codigo": "1.1.1", "nombre": "Tarea A" }]
      }
    ]
  },
  "actividadesAtrasadas": [
    { "id": 10, "codigo": "1.1.1", "nombre": "Diseñar el modelo", "finPlan": "2026-10-01", "avance": 40.00 }
  ],
  "kpi": [
    {
      "id": 4,
      "codigo": "KP-01",
      "nombre": "Avance real",
      "categoria": "PROYECTO",
      "formula": "AVANCE_REAL / AVANCE_PLAN",
      "unidad": "%",
      "sentido": "MAYOR_MEJOR",
      "operadorMeta": "MAYOR_IGUAL",
      "meta": null,
      "valor": 80.0000,
      "cumplimiento": 0.9000,
      "semaforo": "AMARILLO",
      "motivo": "RECALCULO",
      "calculadoEn": "2026-11-09T15:00:00Z",
      "noCalculable": false,
      "alerta": false,
      "alertaMotivo": null
    }
  ]
}
```

Cada elemento de `kpi` es el mismo objeto que `GET /proyectos/{id}/kpi` (`KpiEstadoResponse`).
`valor` nulo y `noCalculable: true` significan «No calculable» (sin semáforo).
`alertaMotivo` es `ROJO` o `EMPEORA`, igual que en [kpi.md](kpi.md).
Cada elemento de `alertas` es el mismo objeto que `GET /proyectos/{id}/sobreasignaciones`.

## Tablero de cambio (C-12, esquema aún abierto)

Conteos por estado V21; en evaluación / implementación; cerrados; rechazados; atrasados;
avance promedio de etapas; riesgos altos/críticos; costos de cerradas; adopción (etapa 10);
efectividad (`resultadoEvaluacion`); alertas (atraso, bloqueo, KPI en rojo). KPI de cambio: B-17.
El JSON lo fija el PR de C-12.
