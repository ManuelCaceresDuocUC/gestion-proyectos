# API de autenticación (C-01)

Base: `/api`. Errores como `{ "codigo": "...", "mensaje": "...", "detalles": [...] }`.
El front **no** es frontera de seguridad: el backend exige JWT y rol.

Roles (V21, ADR-004): `ADMIN` y `USUARIO_PROYECTO`. El issue viejo hablaba de `VISUALIZADOR`; no se usa.

## Rutas públicas

Sin token:

| Método | Ruta | Notas |
|---|---|---|
| POST | `/auth/login` | Body abajo. 200 con token. 401 `CREDENCIALES_INVALIDAS`. 400 `VALIDACION` |
| GET | `/health` | 200 `{ "status": "ok" }` |
| GET | `/swagger-ui.html`, `/v3/api-docs/**` | Documentación |
| OPTIONS | `/**` | CORS |

El resto de `/api/**` pide `Authorization: Bearer <token>`. Sin token o token inválido → 401 `NO_AUTENTICADO`.

## Login

`POST /api/auth/login`

```json
{ "email": "admin@demo.cl", "contrasena": "…" }
```

`email` y `contrasena` obligatorios. El correo se compara en minúsculas.

Respuesta 200 (nunca incluye hash ni contraseña):

```json
{
  "token": "eyJ…",
  "usuario": {
    "id": 1,
    "email": "admin@demo.cl",
    "nombre": "Administrador Demo",
    "rol": "ADMIN"
  }
}
```

El token es HS256. Claim `sub` = id, más `email` y `rol`. Caduca a los 480 minutos (`app.jwt.expira-minutos`).
Se firma con `JWT_SECRET` (mínimo 32 caracteres). El valor del secreto no va en este documento.

Clave mala, email desconocido o usuario inactivo → el mismo 401 `CREDENCIALES_INVALIDAS`
(`"Credenciales inválidas."`). No se distingue el caso.

## Quién puede qué

| Acción | `ADMIN` | `USUARIO_PROYECTO` |
|---|---|---|
| Leer y editar proyectos, EDT, recursos, Gantt, solicitudes | sí | sí |
| Aplicar una solicitud aprobada al plan | sí | sí |
| Aprobar o rechazar (POST o PUT a `APROBADO`/`RECHAZADO`) | sí | 403 `PROHIBIDO` |
| Escritura de KPI y `/api/usuarios/**` (cuando existan) | sí | 403 `PROHIBIDO` |

Reenviar el mismo `estado` en un PUT (el formulario) no pide ADMIN. El salto *hacia* `APROBADO` o `RECHAZADO` sí.

No hay CRUD de usuarios ni refresh token ni cookies en este recorte.

## Front (C-01, mínimo)

`frontend/src/api/client.ts` pone el Bearer desde `sessionStorage` y, ante 401 (salvo el propio login), limpia la sesión y va a `/login`.

**B-04 (layout y guardas):** no vuelvas a enganchar el JWT en `client.ts`; ya lo hace C-01. El layout, la navegación y las guardas de menú por rol siguen siendo tuyas. C-01 solo dejó un `Salir` y una redirección si no hay sesión.

Permisos de botones: `features/cambios/permisos.ts` (`puedeAprobar` = ADMIN), `features/proyectos/permisos.ts` y `features/gantt/permisos.ts` (editar = ambos autenticados).

## Seed

Con `APP_SEED_ENABLED=true` se crean, por email y sin duplicar, `admin@demo.cl` (`ADMIN`) y `usuario@demo.cl` (`USUARIO_PROYECTO`). La contraseña está en `db/seed/caso-prueba.json`; no se loguea ni se reescribe el hash si el usuario ya existe.

## Errores

| Código | HTTP | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | Falta email o contraseña, o el email no es válido |
| `CREDENCIALES_INVALIDAS` | 401 | Login fallido (mismo mensaje en todos los casos) |
| `NO_AUTENTICADO` | 401 | Falta Bearer o el token no es válido |
| `PROHIBIDO` | 403 | Autenticado, sin el rol exigido |
