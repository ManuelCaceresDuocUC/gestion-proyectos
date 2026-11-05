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

Respuesta: `204 No Content`.

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
