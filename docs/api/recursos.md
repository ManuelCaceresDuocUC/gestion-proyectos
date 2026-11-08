# API de recursos

Base: `/api`. Los errores usan `{ "codigo", "mensaje", "detalles" }`.

## Tipos de recurso

### Listar tipos

```http
GET /api/tipos-recurso
```

### Obtener un tipo

```http
GET /api/tipos-recurso/{id}
```

### Crear un tipo

```http
POST /api/tipos-recurso
Content-Type: application/json
```

```json
{
  "nombre": "Profesional",
  "unidad": "hora"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "nombre": "Profesional",
  "unidad": "hora"
}
```

### Actualizar un tipo

```http
PUT /api/tipos-recurso/{id}
Content-Type: application/json
```

Usa el mismo cuerpo de la creación.

### Eliminar un tipo

```http
DELETE /api/tipos-recurso/{id}
```

Respuesta: `204 No Content`.

Si tiene recursos asociados, responde `409 TIPO_RECURSO_EN_USO`.

## Recursos y profesionales

### Listar recursos

```http
GET /api/recursos?soloActivos=true
```

`soloActivos` es opcional y por defecto vale `true`.

### Obtener un recurso

```http
GET /api/recursos/{id}
```

### Crear un recurso

```http
POST /api/recursos
Content-Type: application/json
```

```json
{
  "tipoRecursoId": 1,
  "nombre": "Ana Pérez",
  "tarifa": 25000.00,
  "capacidadHorasDia": 8.0,
  "activo": true
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "tipoRecursoId": 1,
  "nombre": "Ana Pérez",
  "tarifa": 25000.00,
  "capacidadHorasDia": 8.0,
  "activo": true
}
```

`tarifa` debe ser mayor o igual a cero. La capacidad diaria puede ser nula para recursos que no sean profesionales.

### Actualizar un recurso

```http
PUT /api/recursos/{id}
Content-Type: application/json
```

Usa el mismo cuerpo de la creación.

### Eliminar un recurso

```http
DELETE /api/recursos/{id}
```

Respuesta: `204 No Content`. Si el recurso no tiene asignaciones, se elimina físicamente. Si tiene asignaciones,
se conserva para mantener el historial y solo se marca como `activo: false`.

## Asignaciones

### Listar asignaciones de una tarea

```http
GET /api/asignaciones/tarea/{tareaId}
```

La tarea debe existir. Responde `404 NO_ENCONTRADO` si no existe.

### Obtener una asignación

```http
GET /api/asignaciones/{id}
```

### Crear una asignación

```http
POST /api/asignaciones
Content-Type: application/json
```

```json
{
  "tareaId": 15,
  "recursoId": 3,
  "cantidad": 1.00,
  "tarifa": 25000.00,
  "horasSemana": 40.0,
  "semanas": 2.0
}
```

La `tarifa` es opcional. Si se omite, se utiliza la tarifa vigente del recurso.

Las asignaciones solo pueden asociarse a tareas sin subtareas. Si la tarea tiene hijos, responde `409 TAREA_NO_HOJA`.

Al crear, actualizar o eliminar una asignación se ejecuta el recálculo del proyecto. El costo planificado de cada
asignación es `cantidad x tarifa`; el costo real es `cantidadReal x tarifa`. El paso de costos escribe esos valores
en las asignaciones y tareas hoja. Luego la consolidación suma las hojas hacia las actividades, etapas y proyecto sin
duplicar importes. La respuesta del proyecto expone `presupuesto`, `costoPlan` y `costoReal` para comparar el costo
planificado y real contra el presupuesto.

Para B-09, en una asignación de un profesional `cantidad` representa las horas totales de trabajo. Las horas se
distribuyen entre los días hábiles de la tarea según el calendario del proyecto; `horasSemana` y `semanas` son
datos complementarios y no participan en el cálculo de sobreasignación.

Respuesta `201 Created`:

```json
{
  "id": 1,
  "tareaId": 15,
  "recursoId": 3,
  "cantidad": 1.00,
  "tarifa": 25000.00,
  "horasSemana": 40.0,
  "semanas": 2.0,
  "costoPlan": 0.00,
  "cantidadReal": 0.00,
  "costoReal": 0.00
}
```

### Actualizar una asignación

```http
PUT /api/asignaciones/{id}
Content-Type: application/json
```

Utiliza el mismo cuerpo de la creación.

### Consultar alertas de sobreasignación

```http
GET /api/proyectos/{proyectoId}/sobreasignaciones
```

Devuelve una alerta por profesional y fecha cuando la carga diaria supera estrictamente su capacidad. Las alertas
se reemplazan en cada recálculo del proyecto e incluyen las tareas que contribuyen a la carga.

```json
[
  {
    "id": 1,
    "recursoId": 3,
    "profesional": "Ana Pérez",
    "fecha": "2026-11-10",
    "cargaHoras": 12.0000,
    "capacidadHorasDia": 8.0,
    "excesoHoras": 4.0000,
    "tareas": [
      { "id": 15, "codigo": "1.1.1", "nombre": "Tarea A" }
    ]
  }
]
```

### Eliminar una asignación

```http
DELETE /api/asignaciones/{id}
```

Respuesta: `204 No Content`.

## Errores

| Código | HTTP | Situación |
|---|---:|---|
| `NO_ENCONTRADO` | 404 | El tipo, recurso, tarea o asignación no existe |
| `TIPO_RECURSO_DUPLICADO` | 409 | Ya existe un tipo con ese nombre |
| `TIPO_RECURSO_EN_USO` | 409 | Se intenta eliminar un tipo asociado a recursos |
| `TIPO_RECURSO_INEXISTENTE` | 400 | El recurso referencia un tipo inexistente |
| `ASIGNACION_DUPLICADA` | 409 | La tarea ya tiene asignado ese recurso |
| `TAREA_NO_HOJA` | 409 | La tarea tiene subtareas |
| `RECURSO_INACTIVO` | 409 | Se intenta asignar un recurso inactivo |
| `TAREA_DE_OTRO_PROYECTO` | 400 | La nueva tarea pertenece a otro proyecto |
| `PROYECTO_CERRADO` | 409 | El proyecto no admite cambios |
| `VALIDACION` | 400 | Faltan campos o hay valores inválidos |
