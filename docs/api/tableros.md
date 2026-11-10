# API de tableros (B-21, C-12, C-13)

Los tableros **solo leen**. No llaman a `RecalculoService` ni evalúan fórmulas de KPI.
Especificación: `docs/requerimientos.md` (C-02). JWT. `404 NO_ENCONTRADO` si el proyecto no existe.

| Método | Ruta | Dueño | Estado |
|---|---|---|---|
| GET | `/tablero/proyecto/{proyectoId}` | B-21, query C-13 | en `tablero/TableroProyectoController` |
| GET | `/tablero/cambio/{proyectoId}` | C-12, query C-13 | en `tablero/TableroCambioController` |

## Filtros query (C-13)

Query opcional. Ausente o nulo = tablero entero. Varios a la vez: AND.
La forma del JSON no cambia: mismas claves; listas y conteos salen del subconjunto.

| Tablero | Query | Recorta | No recorta |
|---|---|---|---|
| Proyecto | `etapaId`, `desde`, `hasta`, `responsableId` | hitos, `actividadesAtrasadas`, `recursos.alertas` | `avance`, `costos`, `recursos.profesionales`, `recursos.sobreasignados`, `kpi` |
| Cambio | `estado`, `prioridad`, `riesgo`, `responsableId` | solicitudes (y con ellas conteos, etapas, costos, tiempos, efectividad, alertas) | `kpi` (snapshot de categoría `CAMBIO`) |

`etapaId`: id de un nodo de la EDT de ese proyecto (el front manda etapas). Si no está en esa EDT: `404 NO_ENCONTRADO`.
Con etapa, un hito entra si alguna fila de `hito_tarea` cae en esa rama; sin vínculo a la EDT queda fuera.
`desde` / `hasta` (`yyyy-MM-dd`, inclusive): hitos por `fechaPlan`; atrasos por `finPlan`; alertas de sobrecarga por `fecha`. Fecha nula no pasa el rango.
`responsableId` de proyecto: hitos y hojas por `responsable_id`; alertas por `recursoId`.
Enum inválido (`estado`, `prioridad`, `riesgo`): `400 PARAMETRO_INVALIDO`.

Un GET no escribe histórico. El front no suma. Un cambio de filtro reconsulta.
Tras un recálculo o un cambio de solicitud, `/tableros` vuelve a pedir los GET al volver a la pestaña (`visibilitychange`). No hay polling.

Layout: `frontend/src/features/tableros`. `/tableros` muestra las dos, un selector de proyecto y los filtros.

## GET `/tablero/proyecto/{proyectoId}` (B-21)

Query C-13 opcional: recorta hitos, atrasos y `recursos.alertas`. `avance`, `costos`, conteos de profesionales y `kpi` siguen siendo del proyecto entero.

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

## GET `/tablero/cambio/{proyectoId}` (C-12)

Autenticado (`ADMIN` y `USUARIO_PROYECTO`). Query C-13 opcional. `401` sin token.

`enCurso` = `EN_EVALUACION` + `EN_IMPLEMENTACION`. `porEstado` trae las seis claves V21, aunque el conteo sea 0.
Atraso, bloqueo, correctiva y adopción usan las mismas reglas que `VariablesCambio` / `diasAtraso`.
`kpi` son los mismos objetos que `GET /proyectos/{id}/kpi` (`KpiEstadoResponse` en [kpi.md](kpi.md)), filtrados a categoría `CAMBIO`. Vacío si todavía no corrió el paso 60.

```json
{
  "proyectoId": 1,
  "conteos": {
    "total": 5,
    "porEstado": {
      "ABIERTO": 3,
      "EN_EVALUACION": 1,
      "APROBADO": 0,
      "RECHAZADO": 0,
      "EN_IMPLEMENTACION": 1,
      "CERRADO": 0
    },
    "enCurso": 2,
    "atrasados": 0,
    "riesgoAlto": 2,
    "riesgoCritico": 0,
    "conBloqueo": 0,
    "conCorrectiva": 0,
    "adoptados": 0
  },
  "etapas": { "avancePromedio": 0 },
  "costos": { "presupuestoCerradas": 0, "costoRealCerradas": 0 },
  "tiempos": { "diasAtrasoTotal": 0, "diasAtrasoPromedio": 0.00 },
  "efectividad": { "efectivo": 0, "parcial": 0, "noEfectivo": 0, "sinDato": 5 },
  "alertas": [
    { "tipo": "ATRASO", "mensaje": "…", "solicitudCodigo": "SC-01" }
  ],
  "kpi": []
}
```

Alertas (no persistidas): `ATRASO`, `BLOQUEO`, `RIESGO_CRITICO`, `KPI_ROJO`.
`solicitudCodigo` es nulo en las de KPI.

El GET **no** aumenta `kpi_historico` ni llama a `asegurarCatalogo`.
El front no suma lo que el servidor ya mandó. Tras un recálculo, volver a la pestaña (C-13).
