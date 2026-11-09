# Manual de instalación y despliegue (C-17)

Para alguien que **no escribió el código**. El uso de pantallas (proyectos, Gantt, cambios) es B-20
(`docs/manual-usuario.md`). Guion de demo: `docs/pruebas/guion-demo.md`.

Stack: Java 21 / Spring Boot + PostgreSQL 16 + React 18 / Vite. El backend corre en Docker; el front, en Node.

## Qué necesitas

| Herramienta | Para qué |
|---|---|
| Git | Clonar el repo |
| Docker Desktop | PostgreSQL 16 y el API (puerto 8080) |
| Node.js 18 o superior | Frontend en http://localhost:5173 |
| Cuenta GitHub con acceso al repo | Es privado |

Java 21 y Maven **no** hacen falta para el camino Docker. Solo si corres el backend en el IDE.

## 1. Clonar

```powershell
git clone https://github.com/ManuelCaceresDuocUC/gestion-proyectos.git
cd gestion-proyectos
git checkout main
git pull
```

## 2. Variables de entorno

Copia el ejemplo. **No subas** `.env` a git.

```powershell
Copy-Item .env.example .env
```

En bash: `cp .env.example .env`.

Compose, en la raíz, interpola el `.env` (está en `.gitignore`). Valores de ejemplo = `.env.example`; no son secretos de producción.

| Variable | Quién la usa | Para qué |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | contenedor `db` | Base local (default `gestion` / `gestion`) |
| `DB_URL` | Maven/IDE | En `.env.example` apunta a `localhost:5432`. **Compose lo ignora** y pone `jdbc:postgresql://db:5432/${POSTGRES_DB}` |
| `DB_USER` / `DB_PASSWORD` | backend | En Compose = `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| `JWT_SECRET` | backend | Firma HS256; **mínimo 32 caracteres** (`JwtService`). En Render se genera; no lo publiques |
| `APP_SEED_ENABLED` | backend | `true` (default en Compose y `application.yml`) carga `EEC-2026` y los dos usuarios demo |
| `CORS_ORIGINS` | backend | Orígenes del front, separados por coma. Local: `http://localhost:5173` |
| `VITE_API_URL` | build del front | Vacío en local (Vite proxea `/api` a 8080). En Render: URL del API **sin** `/api` |
| `PORT` | backend | No está en `.env.example`. Default 8080; Render Docker suele inyectarla |

`DB_HOST`, `DB_PORT` y `DB_NAME` **no** van en `.env.example`. El API las usa solo si no hay `DB_URL` (Render).

## 3. Levantar en local

En la raíz del repo (donde está `docker-compose.yml`):

```powershell
docker compose up --build
```

Espera a que el API responda (el primer build baja Maven y tarda). Comprueba en el navegador
http://localhost:8080/api/health → `{"status":"ok"}`. En PowerShell, `curl` es otro comando; usa el navegador
o `Invoke-RestMethod http://localhost:8080/api/health`. Swagger: http://localhost:8080/swagger-ui.html

**Otra terminal**, frontend:

```powershell
cd frontend
npm install
npm run dev
```

Abre http://localhost:5173. Si pide login, el seed ya corrió (`APP_SEED_ENABLED=true` por defecto).

Compose **no** incluye el front: son dos procesos. Puertos: 5432 (Postgres), 8080 (API), 5173 (Vite).

## 4. Usuarios demo

Con seed activo se crean, por email y sin duplicar:

| Correo | Rol | Qué puede |
|---|---|---|
| `admin@demo.cl` | `ADMIN` | Todo, incluido Aprobar / Rechazar |
| `usuario@demo.cl` | `USUARIO_PROYECTO` | Trabajar el proyecto; no aprueba ni administra KPI/usuarios |

La contraseña está en el campo `password` de cada usuario en `db/seed/caso-prueba.json`. No se copia aquí. El hash no se reescribe si el usuario ya existe.

Pantalla: **Ingreso** (`/login`) → Correo / Contraseña → **Ingresar**.

## 5. Comprobar que el caso cargó

1. Entra como `admin@demo.cl`.
2. **Proyectos**: código `EEC-2026` («Habilitar un espacio de estudio colaborativo»).
3. **Cambios**: elige ese proyecto; 5 solicitudes `SC-01`…`SC-05`.
4. **Recursos y costos**: alerta de P2 en la hoja 1.1.2.

**Hitos**: elija `EEC-2026`; deben aparecer H1–H6 en Pendiente. KPI y tableros: las rutas existen y dicen pendiente (B-15, B-21, C-12). No es un fallo de instalación.

Detalle del seed: `db/seed/README.md`.

## 6. Resetear la demo

Borra volúmenes y vuelve a crear la base (pierdes datos locales):

```powershell
docker compose down -v
$env:APP_SEED_ENABLED = "true"
docker compose up -d --build
```

En Git bash: `scripts/reset-demo.sh`.

Si `EEC-2026` ya estaba a medias, el arranque **no** recrea proyecto/EDT; usa el reset de arriba.

## 7. Despliegue (Render)

Lo armó B-02 con [`render.yaml`](../render.yaml). Tras cada merge a `main` se redeploya.

| Servicio | URL |
|---|---|
| Front | https://gestion-proyectos-frontend.onrender.com |
| API (health) | https://gestion-proyectos-api.onrender.com/api/health |
| Postgres | privada; solo el API |

Blueprint (si se crea de nuevo):

- API `gestion-proyectos-api` (Docker, health `/api/health`): `JWT_SECRET` generado, `APP_SEED_ENABLED=true`, `CORS_ORIGINS=https://gestion-proyectos-frontend.onrender.com`, `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` desde la base `gestion-proyectos-db` (Postgres 16). No define `DB_URL`.
- Front `gestion-proyectos-frontend` (estático, `npm ci && npm run build` → `dist`): `VITE_API_URL=https://gestion-proyectos-api.onrender.com` (sin `/api`; el cliente agrega `/api`). Un cambio de `VITE_*` exige rebuild del front.

No pegues secretos de Render en el repo ni en el chat del grupo.

## 8. Si algo falla

| Síntoma | Qué mirar |
|---|---|
| `health` no responde | Docker Desktop; `docker compose logs backend`; el primer arranque baja Maven y tarda |
| Puerto 8080 ocupado | Otro Java/Jenkins. Para Vite: `$env:API_PROXY = "http://localhost:8081"` (o el puerto real) |
| Front sin datos / CORS | `CORS_ORIGINS` con el origen exacto del navegador; en local `http://localhost:5173` |
| No aparece `EEC-2026` | `APP_SEED_ENABLED=true` y reset (`down -v`) |
| 401 al entrar | Email y contraseña del JSON; el usuario se crea solo con el seed |
| 403 al aprobar | Entra como `admin@demo.cl` |
| Tests Maven | En CI el seed va `false`. Local necesitan Postgres con las credenciales de `.env` |

## Lista de comprobación (quien prueba el manual)

- [ ] Clonó, copió `.env`, levantó Compose y el front **solo con estos pasos**.
- [ ] `http://localhost:8080/api/health` responde `{"status":"ok"}`.
- [ ] Entró con `admin@demo.cl` y vio `EEC-2026`.
- [ ] (Opcional) Abrió la URL de Render y el login funciona.
