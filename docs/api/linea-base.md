# API de línea base y comparación plan vs real (A-14)

Base: `/api/proyectos/{id}`. Errores como `{ "codigo": "...", "mensaje": "...", "detalles": [...] }`.

## Qué es la línea base
Una copia del plan vigente (fechas y costos) que sirve de referencia. Al fijarla, cada tarea guarda `inicioBase`,
`finBase` y `costoBase`, y el proyecto `fechaFinBase`. Después, cualquier cambio (duraciones, dependencias, solicitudes
aprobadas, recursos) modifica solo el **plan vigente** (`inicioPlan`, `finPlan`, `costoPlan`); la base no se mueve
hasta que se fije una nueva versión.

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/linea-base` | 201 `{id, proyectoId, version, motivo, creadaEn, fechaFin, costoTotal}`. Body opcional: `{"motivo": "..."}` |
| GET | `/lineas-base` | versiones del proyecto, la más reciente primero |
| GET | `/comparacion?hoy=yyyy-MM-dd` | base / plan / real por tarea y resumen del proyecto. `hoy` es opcional (por defecto la fecha actual; sirve para simular en la demo) |

Fijar una línea base recalcula el proyecto justo antes, para que la base y el plan sean consistentes. Errores:
`SIN_PLAN` (409, falta fecha de inicio o tareas con fechas), `PROYECTO_CERRADO` (409, proyecto terminado).

## Comparación
`GET /comparacion` devuelve `{versionLineaBase, fijadaEn, resumen, tareas}`; `versionLineaBase` es nula si todavía
no hay base (las desviaciones salen nulas). Las tareas vienen en orden de código EDT (1, 1.1, 1.2, 1.10...).

Cada tarea incluye fechas base, plan y real, avance, costos y estas desviaciones:
- `desviacionInicioDias`, `desviacionFinDias`: plan contra base, en **días hábiles**; positivo = más tarde que lo comprometido.
- `desviacionFinRealDias`: fecha real de término contra base (nula si aún no termina).
- `desviacionCostoPlan` = `costoPlan - costoBase`; `desviacionCostoReal` = `costoReal - costoBase` (positivo = más caro).
- `atrasada`: terminó después de lo planificado, o no ha terminado, no está al 100 % y su `finPlan` ya pasó.

El `resumen` trae fin base y fin plan del proyecto, costos base / plan / real (la base suma solo las etapas, sin
duplicar), sus desviaciones y el avance. Las tareas creadas después de fijar la base salen sin base ni desviaciones.
