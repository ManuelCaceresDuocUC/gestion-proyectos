# Seed: caso de prueba

Datos ficticios para la demo y las pruebas. Maven los empaqueta en el backend como `classpath:seed/`.

## Archivos

- `caso-prueba.json`: proyecto **EEC-2026** «Habilitar un espacio de estudio colaborativo» (derivado de
  `Actividad_Gantt_Hitos.xlsx`). El JSON ya trae el caso mínimo de la rúbrica; el cargador va por recortes.
- `kpi-definiciones.json`: 8 KPI de proyecto e hitos y 13 de gestión del cambio. **Aún no se cargan** (C-11).

## Recorte v1 (C-07)

Con `APP_SEED_ENABLED=true`, `SeedRunner` llama a `SeedService` y crea los datos **con los servicios**
(`TipoRecursoService`, `RecursoService`, `ProyectoService`, `TareaService`). Las fechas las calcula el motor; no hay SQL a mano.

| Qué | Cuenta | Destino |
|---|---|---|
| Proyecto `EEC-2026` | 1 | `EN_EJECUCION`, LMXJV, 2026-09-07, responsable P1 |
| Tipos de recurso | 4 | Profesional, Equipamiento, Material, Servicio externo |
| Profesionales | 5 | P1…P5 |
| Recursos | 7 | E1–E3, M1–M2, S1–S2 |
| EDT | 4 etapas, 12 actividades, 20 subactividades | `TareaService.crear` |

Reglas:

- Si `EEC-2026` ya existe, no se vuelve a cargar.
- `duracion_semanas` → `duracionDias` = semanas × 5 (calendario LMXJV).
- La actividad **1.4** es hoja (duración propia, sin hijos).
- Los tipos se reutilizan por nombre (no falla si ya están).
- `referencia_semana` es solo orientativa.

## Todavía no (C-11 / C-01)

El JSON incluye usuarios, dependencias, asignaciones, hitos, solicitudes y KPI. **C-07 no los crea.**

## Cómo activarlo

| Entorno | Flag |
|---|---|
| Docker Compose | `APP_SEED_ENABLED=true` (default) |
| Backend local | `APP_SEED_ENABLED=true` (default en `application.yml`) |
| Tests y CI | `false` (no persiste el caso al levantar el contexto) |

Para recargar desde cero: `scripts/reset-demo.sh` (borra volúmenes y levanta de nuevo). En PowerShell:

```powershell
docker compose down -v
$env:APP_SEED_ENABLED = "true"
docker compose up -d --build
```

Código: `backend/src/main/java/cl/proyecto/gestion/seed/`.
