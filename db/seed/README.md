# Seed: caso de prueba

Datos ficticios para la demo y las pruebas. Maven los empaqueta en el backend como `classpath:seed/`.

## Archivos

- `caso-prueba.json`: proyecto **EEC-2026** «Habilitar un espacio de estudio colaborativo» (derivado de
  `Actividad_Gantt_Hitos.xlsx`). El JSON ya trae el caso mínimo de la rúbrica; el cargador va por recortes.
- `kpi-definiciones.json`: 8 KPI de proyecto e hitos y 13 de gestión del cambio. **Este seed no los inserta.**
  Los crea `KpiService.asegurarCatalogo()` en el recálculo (C-12).

## Recorte C-01 (usuarios)

Independiente del proyecto: si el email ya está, esa fila no se toca.

| Qué | Cuenta | Destino |
|---|---|---|
| Usuarios demo | 2 | `admin@demo.cl` (`ADMIN`), `usuario@demo.cl` (`USUARIO_PROYECTO`) |

- La contraseña del JSON se guarda con BCrypt. Los logs no imprimen contraseña ni hash.
- No reescribe el hash si el usuario ya existe.

## Recorte v1 (C-07 + B-09)

Con `APP_SEED_ENABLED=true`, `SeedRunner` llama a `SeedService` y crea los datos **con los servicios**.
Las fechas las calcula el motor; no hay SQL a mano.

| Qué | Cuenta | Destino |
|---|---|---|
| Proyecto `EEC-2026` | 1 | `EN_EJECUCION`, LMXJV, 2026-09-07, responsable P1 |
| Tipos de recurso | 4 | Profesional, Equipamiento, Material, Servicio externo |
| Profesionales | 5 | P1…P5 |
| Recursos | 7 | E1–E3, M1–M2, S1–S2 |
| EDT | 4 etapas, 12 actividades, 20 subactividades | `TareaService.crear` |
| Asignaciones | las de las hojas del JSON (39) | `AsignacionService.crear` (B-09 adelantó este recorte) |

Reglas:

- Si `EEC-2026` ya existe, **no se vuelve a crear** el proyecto, la EDT ni las asignaciones.
- `duracion_semanas` → `duracionDias` = semanas × 5 (calendario LMXJV).
- La actividad **1.4** es hoja (duración propia, sin hijos).
- Los tipos se reutilizan por nombre (no falla si ya están).
- `referencia_semana` es solo orientativa.
- **Alerta de demo (B-09):** P2 en la subactividad 1.1.2 tiene `cantidad` 45 h en 5 días hábiles
  (9 h/día > capacidad 8). Es una sobreasignación de una sola tarea; sigue existiendo con las
  dependencias de C-11.

## Recorte v2 (C-11)

Si el proyecto ya está (seed C-07 viejo o v1 recién creado), C-11 completa **por capas**. No duplica
asignaciones.

| Qué | Cuenta | Destino |
|---|---|---|
| Dependencias | 35 | `DependenciaService.crear` (FS, SS, FF, SF; hay desfase negativo) |
| Línea base | 1 | `LineaBaseService.fijar` **después** de las deps y **antes** de las solicitudes |
| Solicitudes de cambio | 5 | `SolicitudCambioService.crear` → códigos `SC-01`…, estado **`ABIERTO`**, no aplicadas |

Reglas:

- Si ya hay dependencias / línea base / solicitudes, esa capa se salta.
- Las solicitudes **no recorren el grafo**. C-04 genera las 10 etapas al crear.
- El JSON trae códigos `SC-001` y estados/etapas viejos: se ignoran. El alta siempre es `ABIERTO`.
- Tipos del JSON viejo: Tecnología → `TECNOLOGIA`; Personas y Proceso → `ORGANIZACION`.
  Prioridad, riesgo e impacto (Alta, Crítica, Medio…) se normalizan a los enums V21.
- Cada solicitud queda ligada a la tarea por código (`3.1.1`, etc.) y al responsable por clave (P1…).

## Hitos (B-11)

Si el proyecto todavía no tiene hitos, el seed crea H1–H6 con `HitoService`. Quedan en `PENDIENTE`,
con la ficha del JSON (logro, actividades, evidencia, indicador, criterio, validador y acción)
y sin fecha real. La semana del JSON es la propuesta de la planilla y se guarda como el último día
hábil de esa semana. Si el motor dejó el término de las actividades después de ese viernes, el hito
queda en la semana de ese término: la fecha prevista no puede ser anterior.

Si el proyecto ya tiene algún hito, esta capa no se repite.

## Definiciones KPI

El JSON incluye KPI. **Este seed no los crea.** El paso 60 del recálculo inserta KP-01…08 y KC-01…13.
El tablero de cambio (C-12) solo lee el snapshot.

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

Si `EEC-2026` ya estaba de un seed C-07 (sin dependencias ni solicitudes), **no hace falta reset**:
con `APP_SEED_ENABLED=true` el arranque aplica solo las capas que falten.

Código: `backend/src/main/java/cl/proyecto/gestion/seed/`.
