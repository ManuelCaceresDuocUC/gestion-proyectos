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

## Estructura

```
backend/    API Spring Boot (monolito modular)      db/seed/   caso de prueba y KPI (JSON)
frontend/   SPA React + Vite + TypeScript           docs/      requerimientos, modelo, gestión, pruebas
.github/    plantillas, CODEOWNERS y CI              scripts/   utilidades (reset de demo)
```

## Reglas de trabajo (resumen)

- `main` está protegida: todo entra por Pull Request con 1 aprobación y CI en verde.
- Ramas: `feat/<n°issue>-descripcion`, `fix/<n°issue>-descripcion`. Commits: `feat:`, `fix:`, `docs:`, `test:`.
- Todo PR referencia su issue (`Closes #N`). Los detalles están en la guía de desarrollo.
