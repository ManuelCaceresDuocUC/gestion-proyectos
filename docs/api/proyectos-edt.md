# API de proyectos y EDT (A-04, A-05)

Base: `/api`. Estados de proyecto: `PLANIFICADO`, `EN_EJECUCION`, `SUSPENDIDO`, `TERMINADO`. Errores siempre como `{ "codigo": "...", "mensaje": "...", "detalles": [...] }`.

## Proyectos
| Método | Ruta | Notas |
|---|---|---|
| GET | `/proyectos?estado=&responsableId=` | más recientes primero |
| POST | `/proyectos` | 201. `codigo`, `nombre`, `presupuesto` obligatorios. Defaults: estado PLANIFICADO, moneda CLP, días LMXJV |
| GET | `/proyectos/{id}` | 404 si no existe |
| PUT | `/proyectos/{id}` | 409 `PROYECTO_CERRADO` si está terminado |
| POST | `/proyectos/{id}/cerrar` | estado TERMINADO, fija fecha fin real |
| DELETE | `/proyectos/{id}` | 204. Solo en estado PLANIFICADO; si no, 409 `PROYECTO_NO_ELIMINABLE` |

## EDT (3 niveles: etapa, actividad, subactividad)
| Método | Ruta | Notas |
|---|---|---|
| GET | `/proyectos/{id}/edt` | árbol anidado (`hijos`) |
| POST | `/proyectos/{id}/tareas` | 201. `parentId` nulo = etapa; el tipo y el código (1, 1.1, 1.1.1) se deducen solos |
| GET/PUT | `/tareas/{id}` | PUT edita nombre, descripción, duración (días hábiles), responsable, avance |
| DELETE | `/tareas/{id}` | 204; borra descendientes y renumera hermanos |
| POST | `/tareas/{id}/mover` | body `{"posicion": n}` (1 = primera); renumera |

Códigos de error: `CODIGO_DUPLICADO`, `PROYECTO_CERRADO`, `PROYECTO_NO_ELIMINABLE`, `USAR_CERRAR`, `JERARQUIA_INVALIDA`, `PADRE_DE_OTRO_PROYECTO`, `POSICION_INVALIDA`, `TAREA_REFERENCIADA`, `VALIDACION`, `NO_ENCONTRADO`.

Las fechas plan, costos y avance consolidado los llena el recálculo (A-06, A-12); no se envían por API.
