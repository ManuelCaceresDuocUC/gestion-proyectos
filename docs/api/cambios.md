# API de solicitudes de cambio (C-03, C-04, C-08, C-09, C-10)

Base: `/api`. Errores como `{ "codigo": "...", "mensaje": "...", "detalles": [...] }`.
Fechas en ISO-8601 (`yyyy-MM-dd`); instantes en ISO-8601 UTC.

Este contrato cubre **CRUD, filtros, las 10 etapas, transiciones de estado, aprobar/rechazar
y aplicar una solicitud aprobada al plan vigente**. El detalle en el front (C-10) consume esas
rutas. No guarda evidencias ni expone el GET del historial (C-05).
Hoy las rutas están abiertas (`permitAll`); C-01 agregará JWT. El front hoy muestra aprobar a
cualquiera (`puedeAprobar()`); en C-01 será solo ADMIN.

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
| POST | `/cambios/{id}/aprobar` | 200. Sin body. Solo desde `EN_EVALUACION` |
| POST | `/cambios/{id}/rechazar` | 200. Sin body. Solo desde `EN_EVALUACION` |
| POST | `/cambios/{id}/aplicar` | 200. Sin body. Solo desde `APROBADO` y si aún no está aplicada |
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
`impactoCosto`, que conservan el valor anterior si se omiten. El PUT puede cambiar `estado` **solo**
si el salto está en el grafo (abajo). Reenviar el mismo estado es válido y no escribe historial.

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

## Transiciones de estado (C-08)

```
ABIERTO            → EN_EVALUACION
EN_EVALUACION      → ABIERTO | APROBADO | RECHAZADO
APROBADO           → EN_IMPLEMENTACION
EN_IMPLEMENTACION  → CERRADO
RECHAZADO, CERRADO → (terminales)
```

Aprobar y rechazar **solo** desde `EN_EVALUACION` (los `POST` de abajo). Un salto fuera del grafo
responde 409 `TRANSICION_INVALIDA` y no cambia la fila.

Cualquier cambio real de `estado` (PUT, aprobar, rechazar o aplicar) deja una fila en
`cambio_historial` (`campo` = `estado`, `valorAnterior` / `valorNuevo` = el nombre del catálogo).
El GET de esa bitácora es C-05. `usuarioId` queda nulo hasta C-01.

## Aprobar y rechazar

`POST /api/cambios/{id}/aprobar` (sin body):

- Pasa a `APROBADO` y llena `aprobadaEn` (la primera vez).
- **No** pone `aplicada` ni `aplicadaEn` (eso es C-09).

`POST /api/cambios/{id}/rechazar` (sin body): pasa a `RECHAZADO`. No toca `aprobadaEn`.

Si la solicitud no está en `EN_EVALUACION` → 409 `TRANSICION_INVALIDA`.
Si el proyecto está `TERMINADO` → 409 `PROYECTO_CERRADO`.

También se puede llegar a `APROBADO` con un PUT `estado=APROBADO` desde `EN_EVALUACION`; en ese
caso también se llena `aprobadaEn`. Los `POST` son el camino pensado para C-10.

## Aplicar al plan (C-09)

`POST /api/cambios/{id}/aplicar` (sin body). Solo si `estado = APROBADO` y `aplicada = false`.

Efectos, en una sola transacción:

- Tipo **TIEMPO**: suma `impactoDias` a la duración de la tarea hoja (`tareaId`) con
  `CronogramaService.parchear`. Ese servicio dispara el recálculo; **no se escriben fechas a mano**.
  `impactoDias` es un delta (puede ser negativo). La duración resultante no puede ser menor que 0.
- Otros tipos (`ALCANCE`, `COSTO`, `RECURSO`, `TECNOLOGIA`, `ORGANIZACION`): no mueven el plan.
- Marca `aplicada = true` y llena `aplicadaEn` (la primera vez).
- Pasa a `EN_IMPLEMENTACION` (el único salto válido desde `APROBADO`) y deja historial de `estado`.

La **línea base no se toca**. Solo cambia el plan vigente. Un segundo `POST` responde 409 `YA_APLICADA`
y no vuelve a sumar días.

Si el impacto de TIEMPO falla (sin tarea, tarea con hijos, duración negativa), no se marca `aplicada`
ni se cambia el estado.

Hace falta `tareaId` al aplicar un TIEMPO. En el alta/PUT sigue siendo opcional: el 400 aparece
recién en este `POST`.

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
| `TAREA_REQUERIDA` | 400 | Se aplica un TIEMPO sin `tareaId` |
| `DURACION_INVALIDA` | 400 | `duración + impactoDias` quedaría negativa |
| `NO_ENCONTRADO` | 404 | No existe el proyecto, la solicitud, la tarea o la etapa (`numero` fuera de 1–10) |
| `PROYECTO_CERRADO` | 409 | El proyecto está `TERMINADO` (alta, edición, baja, aprobar, rechazar, aplicar o PATCH de etapa) |
| `TRANSICION_INVALIDA` | 409 | El nuevo `estado` no es un paso válido; se aprueba/rechaza fuera de `EN_EVALUACION`; o se aplica fuera de `APROBADO` |
| `YA_APLICADA` | 409 | La solicitud ya fue aplicada al plan |
| `TAREA_CON_HIJOS` | 409 | Se aplica un TIEMPO sobre una etapa o actividad con subtareas |

Un valor fuera de catálogo (`tipo`, `estado`, …) responde 400. Consultar un proyecto cerrado sí
devuelve el listado y las etapas; solo se bloquean las escrituras.

## Pantalla de detalle (C-10)

`/cambios` es el listado; `/cambios?id={id}` es la ficha. El clic en una fila abre el detalle.

Ahí se ven impacto (días y costo), si está `aplicada` / `aplicadaEn`, las 10 etapas (PATCH de
estado) y los botones según el grafo: enviar a evaluación (PUT), aprobar y rechazar (POST),
aplicar al plan (POST C-09) y cerrar (PUT a `CERRADO`). El formulario de alta/edición **no**
cambia el estado. El PUT reenvía `tareaId`. Si el tipo es `TIEMPO` y no hay tarea hoja, Aplicar
queda deshabilitado. Otros tipos al aplicar no mueven el Gantt.

## Fuera de este contrato

No hay `GET /cambios/{id}/historial` ni CRUD de evidencias (C-05). El detalle lo deja dicho.
