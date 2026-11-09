# Sistema de planificación, seguimiento y gestión del cambio

Proyecto integrador del ramo **Gestión de Proyectos de Software** (entrega: **jueves 5 de noviembre de 2026**).
Aplicación web para iniciar y gestionar proyectos: proyectos y EDT, tiempos, recursos y costos, Carta Gantt con
precedencias (FS, SS, FF, SF), KPI configurables, gestión del cambio (10 etapas) y dos tableros de comando.

> **Empieza aquí:** [`docs/GUIA_DESARROLLO.md`](docs/GUIA_DESARROLLO.md) — el caso, qué pide la evaluación,
> y qué issues le tocan a cada desarrollador.
>
> **Instalar y desplegar:** [`docs/instalacion.md`](docs/instalacion.md) (C-17). Uso de pantallas: [`docs/manual-usuario.md`](docs/manual-usuario.md) (B-20).

| Dev | GitHub | Responsabilidad |
|---|---|---|
| A | ManuelCaceresDuocUC | Núcleo de planificación: modelo de datos, proyectos y EDT, motor de dependencias, Gantt, servicio de recálculo |
| B | HeOlivares | Recursos, tiempos y costos, hitos, administrador de KPI, tablero de proyecto, despliegue |
| C | cristianmonsalve14 | Gestión del cambio, tablero de cambio, autenticación y roles, seed del caso de prueba, documentación |

Tablero de gestión del propio desarrollo: pestaña **Projects** del repositorio (`ManuelCaceresDuocUC/gestion-proyectos`).

## Levantar en local

Pasos completos, variables, usuarios demo y Render: [`docs/instalacion.md`](docs/instalacion.md).

```bash
cp .env.example .env
docker compose up --build        # PostgreSQL + backend en http://localhost:8080
cd frontend && npm install && npm run dev   # frontend en http://localhost:5173
```

Comprobación: `http://localhost:8080/api/health` → `{"status":"ok"}`. Swagger: `http://localhost:8080/swagger-ui.html`.

## Flujo mínimo para colaborar

```bash
git clone https://github.com/ManuelCaceresDuocUC/gestion-proyectos.git
cd gestion-proyectos
docker compose up --build
cd frontend && npm install && npm run build
git switch -c feat/<n°issue>-descripcion
git push -u origin feat/<n°issue>-descripcion
```

Después del `push`, abre el Pull Request desde GitHub, conserva la plantilla,
completa el checklist y usa `Closes #<n° del issue>`. La fusión requiere una
aprobación, que puede realizar el propio autor, y el CI en verde.

## Despliegue online

Blueprint en [`render.yaml`](render.yaml) (B-02). Detalle de variables: [`docs/instalacion.md`](docs/instalacion.md).

- Frontend: <https://gestion-proyectos-frontend.onrender.com>
- Backend: <https://gestion-proyectos-api.onrender.com/api/health>

## Estructura

```
backend/    API Spring Boot (monolito modular)      db/seed/   caso de prueba y KPI (JSON)
frontend/   SPA React + Vite + TypeScript           docs/      requerimientos, modelo, gestión, pruebas
.github/    plantillas, CODEOWNERS y CI              scripts/   utilidades (reset de demo)
```

## Reglas de trabajo (resumen)

- Todo entra a `main` por Pull Request, con al menos una aprobación del autor o de otro integrante y el CI en verde; nunca se sube directo a `main`.
- Ramas: `feat/<n°issue>-descripcion`, `fix/<n°issue>-descripcion`. Commits: `feat:`, `fix:`, `docs:`, `test:`.
- Todo PR referencia su issue (`Closes #N`). Los detalles están en la guía de desarrollo.
