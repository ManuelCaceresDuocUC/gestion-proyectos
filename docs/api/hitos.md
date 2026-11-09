# API de hitos (esqueleto T-02 / B-11)

Pendiente de implementación. Tablas `hito` y `hito_tarea` ya existen (V1 + `responsable_id` en V21).
Este archivo fija rutas y campos; el JSON exacto lo publica B-11 en el mismo PR que el controlador.

Duración cero: un hito **no** es una tarea de la EDT. Se asocia a una o más actividades (`hito_tarea`).
No se marca `CUMPLIDO` sin `fechaReal` ni antes de que terminen esas actividades (RF-HIT-02).

Base: `/api`. JWT. Proyecto `TERMINADO` → `409 PROYECTO_CERRADO`.

## Rutas previstas

| Método | Ruta | Notas |
|---|---|---|
| GET | `/proyectos/{proyectoId}/hitos` | lista del proyecto |
| POST | `/proyectos/{proyectoId}/hitos` | 201. Caso mínimo: 6 hitos |
| GET | `/hitos/{id}` | 404 si no existe |
| PUT | `/hitos/{id}` | editar ficha y asociaciones |
| DELETE | `/hitos/{id}` | 204 |

## Campos (modelo)

`codigo` único por proyecto (`H1`…), `logro`, `fechaPlan`, `fechaBase`, `fechaReal`,
`estado` (`PENDIENTE`, `CUMPLIDO`, `NO_CUMPLIDO`), `evidencia`, `indicador`,
`criterioAceptacion`, `validador`, `accionSiNoCumple`, `responsableId` (recurso, V21),
`tareaIds` (actividades asociadas). Semana prevista puede ir en el body si B la persiste;
si no, se deriva de `fechaPlan`.

Errores esperados: `VALIDACION`, `NO_ENCONTRADO`, `PROYECTO_CERRADO`, y uno de negocio
si se marca cumplido antes de tiempo (B-11 elige el código).

El paso `HITOS` (orden 50) corre **dentro** de `RecalculoService` cuando otro cambio dispara el
recálculo. Crear o editar un hito no mueve el Gantt.
