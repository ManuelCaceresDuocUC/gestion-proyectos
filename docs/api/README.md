# Contratos de API (T-02)

Índice de lo que cada módulo **expone hoy** en `main`. Si un PR cambia rutas, DTOs o códigos de
error, actualiza el `.md` de ese módulo en el mismo PR.

Swagger: `/swagger-ui.html` con el backend levantado. Base: `/api`.
C-01: casi todo pide `Authorization: Bearer`. Públicas: `POST /auth/login`, `GET /health`,
Swagger y `OPTIONS`. Detalle en [auth.md](auth.md).

El recálculo **no tiene endpoint**. `RecalculoService.recalcular(proyectoId)` lo invocan los
servicios. Contrato: [recalculo.md](recalculo.md).

## Estado por módulo

| Módulo | Prefijo HTTP | Dueño | Contrato |
|---|---|---|---|
| salud | `/health` | A | pública; `{ "status": "ok" }` |
| auth | `/auth` | C | [auth.md](auth.md) — login JWT; roles `ADMIN` / `USUARIO_PROYECTO` |
| proyectos y EDT | `/proyectos`, `/tareas` | A | [proyectos-edt.md](proyectos-edt.md) |
| calendario | `/no-laborables` | B | [planificacion.md](planificacion.md) (B-07) |
| dependencias y cronograma | `/dependencias`, `PATCH /tareas/{id}` | A | [planificacion.md](planificacion.md) |
| línea base | `/proyectos/{id}/linea-base`, `/lineas-base`, `/comparacion` | A | [linea-base.md](linea-base.md) |
| recursos y asignaciones | `/tipos-recurso`, `/recursos`, `/asignaciones` | B | [recursos.md](recursos.md) |
| sobreasignación | `/proyectos/{id}/sobreasignaciones` | B | [recursos.md](recursos.md) (B-09) |
| solicitudes de cambio | `/proyectos/{id}/cambios`, `/cambios/{id}`, `/etapas`, `/historial`, `/evidencias` | C | [cambios.md](cambios.md) — C-05 en el mismo contrato |
| recálculo (interno) | — | A (+ pasos B/C) | [recalculo.md](recalculo.md) — no hay `POST /proyectos/{id}/recalcular` |
| hitos | `/proyectos/{id}/hitos`, `/hitos/{id}` | B | [hitos.md](hitos.md) — **sin controlador** (B-11) |
| KPI | GET `/proyectos/{id}/kpi`; escrituras `/kpi/**` (ADMIN) | B | [kpi.md](kpi.md); [kpi-variables.md](kpi-variables.md) — **sin controlador** |
| tablero de proyecto | `/tablero/proyecto/{id}` | B | [tableros.md](tableros.md) — **sin controlador** (B-21) |
| tablero de cambio | `/tablero/cambio/{id}` | C | [tableros.md](tableros.md) — **sin controlador** (C-12) |
| usuarios (CRUD) | `/usuarios` | C | **sin controlador**; seed C-01. Matcher ADMIN ya está en SecurityConfig |

Gantt, listados y formularios del front **consumen** estas rutas; no hay API de “pantalla”.

## Convenciones

- JSON en español (`nombre`, `fechaInicio`, `contrasena`).
- Fechas `yyyy-MM-dd`; instantes ISO-8601 UTC.
- Errores: `{ "codigo": "...", "mensaje": "...", "detalles": [...] }` con 400 / 401 / 403 / 404 / 409.
- Un proyecto `TERMINADO` no se escribe (`409 PROYECTO_CERRADO`).
- Códigos de catálogo en MAYÚSCULAS (`ABIERTO`, `FS`, `ADMIN`).
- El front no es frontera: el backend exige JWT y rol.
