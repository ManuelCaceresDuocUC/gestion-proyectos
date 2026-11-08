# Sistema de planificación, seguimiento y gestión del cambio

Proyecto integrador del ramo **Gestión de Proyectos de Software** (entrega: **jueves 5 de noviembre de 2026**).
Aplicación web para iniciar y gestionar proyectos: proyectos y EDT, tiempos, recursos y costos, Carta Gantt con
precedencias (FS, SS, FF, SF), KPI configurables, gestión del cambio (10 etapas) y dos tableros de comando.

> **Empieza aquí:** [`docs/GUIA_DESARROLLO.md`](docs/GUIA_DESARROLLO.md) — el caso, qué pide la evaluación,
> y qué issues le tocan a cada desarrollador.

| Dev | GitHub | Responsabilidad |
|---|---|---|
| A | ManuelCaceresDuocUC | Núcleo de planificación: modelo de datos, proyectos y EDT, motor de dependencias, Gantt, servicio de recálculo |
| B | HeOlivares | Recursos, tiempos y costos, hitos, administrador de KPI, tablero de proyecto, despliegue |
| C | cristianmonsalve14 | Gestión del cambio, tablero de cambio, autenticación y roles, seed del caso de prueba, documentación |

Tablero de gestión del propio desarrollo: pestaña **Projects** del repositorio (`ManuelCaceresDuocUC/gestion-proyectos`).

## Levantar en local

```bash
cp .env.example .env
docker compose up --build        # PostgreSQL + backend en http://localhost:8080
cd frontend && npm install && npm run dev   # frontend en http://localhost:5173
```

Comprobación: `http://localhost:8080/api/health` debe responder `{"status":"ok"}`.
Documentación de la API (cuando exista): `http://localhost:8080/swagger-ui.html`.

## Despliegue online

El despliegue definido en [`render.yaml`](render.yaml) publica PostgreSQL, el backend
y el frontend como servicios de Render. Los tres servicios siguen la rama `main` y
se redeployan automáticamente después de cada push aceptado en esa rama.

URLs del entorno:

- Frontend: <https://gestion-proyectos-frontend.onrender.com>
- Backend: <https://gestion-proyectos-api.onrender.com/api/health>
- PostgreSQL: base de datos privada de Render, accesible solo por el backend

Al crear el Blueprint en Render, configura estos valores en el servicio `gestion-proyectos-api`:

| Variable | Uso | Ejemplo local / valor online |
|---|---|---|
| `DB_URL` | URL JDBC completa; opcional si se usan las variables separadas | `jdbc:postgresql://localhost:5432/gestion` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexión a PostgreSQL cuando `DB_URL` no está definida | Los entrega Render |
| `DB_USER`, `DB_PASSWORD` | Credenciales de PostgreSQL | Las entrega Render |
| `JWT_SECRET` | Secreto para firmar tokens; mínimo 32 caracteres | Generar uno exclusivo y no publicarlo |
| `CORS_ORIGINS` | Orígenes permitidos, separados por coma | `https://gestion-proyectos-frontend.onrender.com` |
| `APP_SEED_ENABLED` | Carga el caso de demostración al iniciar | `true` en la demo, `false` en producción |

Para el servicio `gestion-proyectos-frontend`, define `VITE_API_URL` con
`https://gestion-proyectos-api.onrender.com`. Las variables `VITE_*` se incorporan
durante el build, por lo que un cambio requiere un nuevo deploy del frontend.

## Estructura

```
backend/    API Spring Boot (monolito modular)      db/seed/   caso de prueba y KPI (JSON)
frontend/   SPA React + Vite + TypeScript           docs/      requerimientos, modelo, gestión, pruebas
.github/    plantillas, CODEOWNERS y CI              scripts/   utilidades (reset de demo)
```

## Reglas de trabajo (resumen)

- Todo entra a `main` por Pull Request que fusiona su autor cuando el CI está en verde (sin aprobación obligatoria de otro).
- Ramas: `feat/<n°issue>-descripcion`, `fix/<n°issue>-descripcion`. Commits: `feat:`, `fix:`, `docs:`, `test:`.
- Todo PR referencia su issue (`Closes #N`). Los detalles están en la guía de desarrollo.
