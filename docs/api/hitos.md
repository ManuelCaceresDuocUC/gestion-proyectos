# API de hitos (B-11)

Tablas `hito` y `hito_tarea` (V1 + `responsable_id` en V21). No hay migración nueva.
Un hito es un logro verificable de duración cero: no es una tarea de la EDT. Se asocia a una o más
actividades (`hito_tarea`).

La semana prevista no es columna. El cuerpo trae `semanaPrevista` (1 = primera semana desde
`proyecto.fechaInicio`, en días hábiles del calendario del proyecto). El servicio guarda `fechaPlan`
como el último día hábil de esa semana y la respuesta devuelve ambas.

Evidencia y fuente van en `evidencia`. Unidad y criterio van en `criterioAceptacion`.
Quién valida y cuándo va en `validador`. `fechaBase` se lee y no se escribe en este módulo.

Base: `/api`. JWT. Proyecto `TERMINADO` → `409 PROYECTO_CERRADO`.
Crear o editar un hito no llama al recálculo y no mueve el Gantt. El paso `HITOS` (orden 50) sigue reservado para B-16.

## Rutas

| Método | Ruta | Notas |
|---|---|---|
| GET | `/proyectos/{proyectoId}/hitos` | lista del proyecto, por fecha prevista y código |
| POST | `/proyectos/{proyectoId}/hitos` | 201 |
| GET | `/hitos/{id}` | 404 si no existe |
| PUT | `/hitos/{id}` | editar ficha y asociaciones |
| DELETE | `/hitos/{id}` | 204 |

## Cuerpo (POST y PUT)

```json
{
  "codigo": "H1",
  "logro": "Alcance y criterios de aceptación acordados",
  "semanaPrevista": 3,
  "evidencia": "Documento de alcance firmado por dirección",
  "indicador": "Binario: sí si el documento está firmado y tiene criterios medibles",
  "criterioAceptacion": "Sí/No; cumple con documento firmado",
  "validador": "Coordinadora de proyecto, al cierre de la semana 3",
  "accionSiNoCumple": "Reunión extraordinaria para renegociar alcance antes de diseñar",
  "estado": "PENDIENTE",
  "fechaReal": null,
  "responsableId": null,
  "tareaIds": [12]
}
```

`estado`: `PENDIENTE`, `CUMPLIDO`, `NO_CUMPLIDO`.

## Respuesta

Los campos del cuerpo más `id`, `proyectoId`, `fechaPlan`, `fechaBase`, `semanaPrevista` y
`actividades` (`id`, `codigo`, `nombre`, `finPlan`, `finReal`). `tareaIds` repite los id de esas actividades.

## Reglas

- `fechaPlan` (la semana) no puede ser anterior al `finPlan` de ninguna actividad asociada.
- `fechaReal`, si viene, no puede ser anterior al fin real de la actividad o, si todavía no tiene, a su `finPlan`.
- Esas dos reglas responden `409 HITO_ANTES_DE_ACTIVIDADES`. El `mensaje` nombra la actividad y su término. La pantalla lo muestra como alerta y no guarda.
- `CUMPLIDO` exige evidencia no vacía (`400 CUMPLIDO_SIN_EVIDENCIA`), `fechaReal` (`400 CUMPLIDO_SIN_FECHA_REAL`) y que cada actividad asociada tenga `finReal` (`409 HITO_ACTIVIDADES_PENDIENTES`).
- Al menos una actividad del mismo proyecto. Si no tiene `finPlan`: `400 HITO_SIN_TERMINO`.
- Código repetido en el proyecto: `409 CODIGO_DUPLICADO`.
- El seed carga H1–H6 como `PENDIENTE`, sin fecha real. Si el término calculado de las actividades cae después del viernes de la semana del JSON, el hito queda en la semana de ese término.

Otros errores: `VALIDACION`, `NO_ENCONTRADO`, `PROYECTO_CERRADO`, `PROYECTO_SIN_INICIO`, `TAREA_AJENA`.
