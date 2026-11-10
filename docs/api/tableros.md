# API de tableros (B-21, C-12, C-13)

Los tableros **solo leen**. No llaman a `RecalculoService` ni insertan definiciones KPI.
Especificación: `docs/requerimientos.md` (C-02). JWT. `404 NO_ENCONTRADO` si el proyecto no existe.

| Método | Ruta | Dueño | Estado |
|---|---|---|---|
| GET | `/tablero/proyecto/{proyectoId}` | B-21 | sin controlador |
| GET | `/tablero/cambio/{proyectoId}` | C-12 | en `tablero/TableroCambioController` |

Filtros query (C-13, **no** están en C-12):

| Tablero | Filtros |
|---|---|
| Proyecto | `etapaId`, `desde`, `hasta`, `responsableId` |
| Cambio | `estado`, `prioridad`, `riesgo`, `responsableId` |

Layout: `frontend/src/features/tableros`. Dos pantallas. `/tableros` muestra el de cambio (C-12) y un stub «Pendiente de implementar (B-21).».

## GET `/tablero/cambio/{proyectoId}` (C-12)

Autenticado (`ADMIN` y `USUARIO_PROYECTO`). Sin query. `401` sin token.

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
El front no suma lo que el servidor ya mandó. Recargar tras un recálculo es C-13.
