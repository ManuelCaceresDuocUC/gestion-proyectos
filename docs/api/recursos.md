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

## Errores

| Código | HTTP | Situación |
|---|---:|---|
| `NO_ENCONTRADO` | 404 | El tipo o recurso no existe |
| `TIPO_RECURSO_DUPLICADO` | 409 | Ya existe un tipo con ese nombre |
| `TIPO_RECURSO_EN_USO` | 409 | Se intenta eliminar un tipo asociado a recursos |
| `TIPO_RECURSO_INEXISTENTE` | 400 | El recurso referencia un tipo inexistente |
| `VALIDACION` | 400 | Faltan campos o hay valores inválidos |
