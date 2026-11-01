# API de solicitudes de cambio (C-03, C-04)

Base: `/api`. Errores como `{ "codigo": "...", "mensaje": "...", "detalles": [...] }`.
Fechas en ISO-8601 (`yyyy-MM-dd`); instantes en ISO-8601 UTC.

Este contrato cubre **CRUD, filtros y las 10 etapas**. No guarda evidencias ni expone el GET
del historial (C-05), no valida transiciones de la solicitud (C-08) y no aplica el cambio al
plan (C-09). Hoy las rutas están abiertas (`permitAll`); C-01 / C-02 agregarán JWT.

## Catálogos

| Campo | Valores |
|---|---|
| `tipo` | `ALCANCE`, `TIEMPO`, `COSTO`, `RECURSO`, `TECNOLOGIA`, `ORGANIZACION` |
| `estado` | `ABIERTO`, `EN_EVALUACION`, `APROBADO`, `RECHAZADO`, `EN_IMPLEMENTACION`, `CERRADO` |
| `prioridad` | `BAJA`, `MEDIA`, `ALTA`, `CRITICA` |
| `riesgo` | `BAJO`, `MEDIO`, `ALTO`, `CRITICO` |
| `impacto` | `BAJO`, `MEDIO`, `ALTO` |
| `resultadoEvaluacion` | `EFECTIVO`, `PARCIAL`, `NO_EFECTIVO` |
| `estado` de etapa | `PENDIENTE`, `EN_CURSO`, `COMPLETADO`, `BLOQUEADO`, `NO_APLICA` |

## Rutas

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/proyectos/{proyectoId}/cambios` | 200 lista. Filtros opcionales abajo. 404 si el proyecto no existe |
| POST | `/proyectos/{proyectoId}/cambios` | 201. Body: `SolicitudCambioRequest` |
| GET | `/cambios/{id}` | 200. 404 `NO_ENCONTRADO` |
| PUT | `/cambios/{id}` | 200. Mismo body que el alta |
| DELETE | `/cambios/{id}` | 204 |
| GET | `/cambios/{id}/etapas` | 200. Las 10 + calculados. 404 si no existe la solicitud |
| PATCH | `/cambios/{id}/etapas/{numero}` | 200. Body: `{"estado":"EN_CURSO"}`. `numero` es 1–10 |

El listado ordena por `fechaSolicitud` descendente. Cada filtro nulo se ignora; si vienen varios, se combinan (AND).
`desde` y `hasta` aplican a `fechaSolicitud` e incluyen los extremos.

| Query | Tipo | Ejemplo |
|---|---|---|
| `estado` | catálogo | `ABIERTO` |
| `tipo` | catálogo | `COSTO` |
| `prioridad` | catálogo | `ALTA` |
| `responsableId` | número | `3` |
| `desde` | fecha | `2026-10-01` |
| `hasta` | fecha | `2026-10-31` |

Ejemplo: `GET /api/proyectos/1/cambios?tipo=TIEMPO&estado=ABIERTO&desde=2026-10-01`.

## Alta y edición

Obligatorios en el body: `nombre` (hasta 200) y `tipo`. El resto es opcional.

El servidor asigna `codigo` (`SC-01`, `SC-02`, … por proyecto), `aplicada=false` y, en el alta,
`estado=ABIERTO`. Esos tres no se aceptan desde el cliente: si el POST manda otro `estado`, se ignora.
`proyectoId` sale de la URL.

Si no vienen, el alta usa: `fechaSolicitud` = hoy, `prioridad=MEDIA`, `riesgo=MEDIO`, `impacto=MEDIO`,
`impactoDias=0`, `impactoCosto=0`.

El PUT **no es un PATCH**. Hay que reenviar `nombre` y `tipo`. Los campos opcionales que no vienen
quedan en `null`, salvo `fechaSolicitud`, `prioridad`, `riesgo`, `impacto`, `estado`, `impactoDias` e
`impactoCosto`, que conservan el valor anterior si se omiten. El PUT sí puede cambiar `estado`
(cualquier valor del catálogo); C-08 restringirá las transiciones.

`tareaId` es opcional y, si viene, debe existir y pertenecer al mismo proyecto.

Al crear se insertan las **10 etapas** en `PENDIENTE`. El cliente no las manda.

Campos que el cliente no envía y solo se leen: `id`, `proyectoId`, `codigo`, `aprobadaEn`,
`aplicada`, `aplicadaEn`, `creadoEn`, `diasAtraso`, `etapasCompletas`, `avance`, `etapasBloqueadas`.

### Body de entrada (`SolicitudCambioRequest`)

```json
{
  "nombre": "Ampliar plazo de construcción",
  "descripcion": "Reprogramar por atraso de proveedor",
  "solicitante": "Ana Pérez",
  "responsableId": 3,
  "fechaSolicitud": "2026-10-20",
  "inicioPrevisto": "2026-11-03",
  "finPrevisto": "2026-11-14",
  "finReal": null,
  "prioridad": "ALTA",
  "tipo": "TIEMPO",
  "riesgo": "MEDIO",
  "impacto": "ALTO",
  "estado": "EN_EVALUACION",
  "tareaId": 12,
  "impactoDias": 5,
  "impactoCosto": 1500000,
  "impactoAlcance": "Sin cambio de alcance",
  "impactoRecursos": "Un analista extra",
  "resultadoEvaluacion": null,
  "presupuestoPrevisto": 2000000,
  "costoReal": null,
  "accionCorrectiva": null,
  "resultado": null
}
```

### Respuesta (`SolicitudCambioResponse`)

Los mismos campos del body más `id`, `proyectoId`, `codigo`, `aprobadaEn`, `aplicada`,
`aplicadaEn`, `creadoEn`, `diasAtraso`, `etapasCompletas`, `avance` y `etapasBloqueadas`.
El listado **no** incluye el arreglo de etapas; eso va en `GET /cambios/{id}/etapas`.

## `diasAtraso`

Calculado en cada respuesta; **no se persiste**. Días corridos (no hábiles). Mínimo 0.

- Sin `finPrevisto` → `0`.
- Con `finReal` → `finReal - finPrevisto`.
- Sin `finReal` y estado `CERRADO` o `RECHAZADO` → `0`.
- Sin `finReal` y cualquier otro estado → `hoy - finPrevisto`.

## Etapas (C-04)

Al `POST` de la solicitud se crean siempre estas 10, en `PENDIENTE`:

1. Necesidad · 2. Evaluación · 3. Riesgos · 4. Planificación · 5. Comunicación inicial ·
6. Implementación · 7. Seguimiento · 8. Resultados corto plazo · 9. Correctivas ·
10. Adopción cultural

Cualquier etapa puede pasar a cualquiera de los 5 estados (no hay secuencia). El mismo estado
no escribe historial. Un cambio de estado deja una fila en `cambio_historial`
(`campo` = `etapa.{n}.estado`); el GET de esa bitácora es C-05. `actualizadoPor` / `usuarioId`
quedan nulos hasta C-01.

`GET /cambios/{id}/etapas` responde:

```json
{
  "etapas": [
    { "id": 1, "solicitudId": 8, "numero": 1, "nombre": "Necesidad",
      "estado": "COMPLETADO", "actualizadoEn": "2026-11-01T15:00:00Z", "actualizadoPor": null }
  ],
  "etapasCompletas": 1,
  "avance": 10,
  "etapasBloqueadas": 0
}
```

## Avance de etapas

Calculado; **no se persiste**. Porcentaje 0–100.

- `etapasCompletas` = cuántas están `COMPLETADO`
- `avance` = completas × 10  (es decir, completas / 10 × 100)
- `etapasBloqueadas` = cuántas están `BLOQUEADO`
- `NO_APLICA` no suma al avance

## Errores

| Código | HTTP | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | Falta `nombre`, `tipo` o `estado` de etapa, o un campo no cumple el tamaño |
| `TAREA_DE_OTRO_PROYECTO` | 400 | `tareaId` existe pero es de otro proyecto |
| `NO_ENCONTRADO` | 404 | No existe el proyecto, la solicitud, la tarea o la etapa (`numero` fuera de 1–10) |
| `PROYECTO_CERRADO` | 409 | El proyecto está `TERMINADO` (alta, edición, baja o PATCH de etapa) |

Un valor fuera de catálogo (`tipo`, `estado`, …) responde 400. Consultar un proyecto cerrado sí
devuelve el listado y las etapas; solo se bloquean las escrituras.

## Fuera de este contrato

No hay `GET /cambios/{id}/historial` ni CRUD de evidencias (C-05). No hay
`POST /cambios/{id}/aprobar`, `/rechazar` ni `/aplicar`. `aplicada` y `aplicadaEn` quedan
en falso / nulo hasta C-09.
