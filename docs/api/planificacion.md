# API de planificación: dependencias y cronograma (A-09, A-13)

Base: `/api`. Errores como `{ "codigo": "...", "mensaje": "...", "detalles": [...] }`.
Toda operación de esta página **recalcula el proyecto** y responde con `cambios`: las tareas cuyas fechas plan
se movieron (`tareaId, codigo, nombre, inicioAntes, finAntes, inicioDespues, finDespues`), incluidas etapas y actividades.

## Dependencias
Solo entre tareas **sin subtareas** (las fechas de etapas y actividades se calculan desde sus hijos).
Tipos: `FS`, `SS`, `FF`, `SF`. `desfaseDias` en días hábiles; negativo = adelanto; si no viene, 0.

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/proyectos/{id}/dependencias` | lista de `{id, predecesoraId, sucesoraId, tipo, desfaseDias}` |
| POST | `/proyectos/{id}/dependencias` | 201 `{dependencia, cambios}`. Body: `{predecesoraId, sucesoraId, tipo, desfaseDias}` |
| PUT | `/dependencias/{id}` | `{dependencia, cambios}`. Body: `{tipo, desfaseDias}` (para cambiar las tareas, eliminar y crear otra) |
| DELETE | `/dependencias/{id}` | `{cambios}` |

Errores: `CICLO_EN_DEPENDENCIAS` (409, el mensaje trae el ciclo, por ejemplo
`1.3 Construccion → 1.1 Analisis → 1.2 Diseno → 1.3 Construccion`), `DEPENDENCIA_A_SI_MISMA` (400),
`DEPENDENCIA_DUPLICADA` (409), `DEPENDENCIA_EN_PADRE` (409), `TAREA_DE_OTRO_PROYECTO` (400),
`PROYECTO_CERRADO` (409, proyecto terminado). Si algo falla no se guarda nada.

Una etapa o actividad que ya tiene dependencias no admite subtareas nuevas: 409 `TAREA_CON_DEPENDENCIAS`.

## Cambiar duración, fechas reales y avance
`PATCH /tareas/{id}` con cualquiera de: `duracionDias` (días hábiles, ≥ 0), `inicioReal`, `finReal` (`yyyy-MM-dd`),
`avance` (0 a 100). Los campos que no vienen no se tocan. Respuesta: `{tarea, cambios}`.

Errores: `PATCH_VACIO` (400), `TAREA_CON_HIJOS` (409, esos valores se calculan desde las subtareas),
`FECHAS_REALES_INVALIDAS` (400), `PROYECTO_CERRADO` (409).

Ejemplo: `PATCH /api/tareas/12  {"duracionDias": 6}` → la tarea 12 y todas sus sucesoras se desplazan, y la respuesta
lista cada una con su fecha anterior y la nueva.
