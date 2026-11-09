# Contrato de recálculo (T-02 / A-12)

No hay `POST /api/proyectos/{id}/recalcular`. El único punto es

`RecalculoService.recalcular(Long proyectoId)`

en `planificacion.recalculo`. Corre en **una transacción**. Spring inyecta todos los
`PasoRecalculo` ordenados por `@Order` (`OrdenRecalculo`). Si un paso falta (aún no hay bean),
ese hueco no se ejecuta; los demás sí.

Los tableros **solo leen** lo ya persistido. Nadie recalcula por su cuenta ni escribe fechas
plan a mano.

Detalle de consolidación: ADR-003. Semántica FS/SS/FF/SF: ADR-002.

## Quién dispara

| Disparo | Servicio | Ruta HTTP típica |
|---|---|---|
| Edición de proyecto (p. ej. fecha de inicio) | `ProyectoService.actualizar` | `PUT /proyectos/{id}` (el alta no recalcula) |
| Alta, edición, baja o mover tarea | `TareaService` | `/proyectos/{id}/tareas`, `/tareas/{id}` |
| Duración, fechas reales, avance de una hoja | `CronogramaService.parchear` | `PATCH /tareas/{id}` |
| Alta, edición o baja de dependencia | `DependenciaService` | `/dependencias` |
| Fijar línea base (recalcula **antes** de copiar) | `LineaBaseService` | `POST /proyectos/{id}/linea-base` |
| Día no laborable | `NoLaborableService` | `/no-laborables` |
| Alta, edición o baja de asignación | `AsignacionService` | `/asignaciones` |
| Aplicar solicitud **TIEMPO** | `CronogramaService.parchear` (lo llama C-09) | `POST /cambios/{id}/aplicar` |

Aplicar un tipo que no es `TIEMPO` **no** llama al recálculo (C-02 / C-09).

La respuesta HTTP de cronograma y dependencias trae `cambios`: lista de
`{tareaId, codigo, nombre, inicioAntes, finAntes, inicioDespues, finDespues}`
(`ResultadoRecalculo` / `CambioFecha`). El recálculo en sí no es un DTO de API.

## Interfaz del paso

```java
public interface PasoRecalculo {
    void ejecutar(Long proyectoId);
}
```

Cada módulo registra un `@Component` con `@Order(OrdenRecalculo.…)`.
Reglas: **idempotente**; si lanza `ApiException` se propaga; cualquier otra
`RuntimeException` → `500 RECALCULO_FALLIDO` y rollback de **todos** los pasos.
Ciclo en el motor → `409 CICLO_EN_DEPENDENCIAS`.

## Orden (`OrdenRecalculo`)

| Orden | Constante | Clase hoy | Dueño | Estado |
|---|---|---|---|---|
| 10 | `MOTOR_FECHAS` | `PasoMotorFechas` | A | en main |
| 20 | `CONSOLIDACION` | `PasoConsolidacion` | A | en main |
| 30 | `COSTOS` | `PasoCostos` | B | en main |
| 35 | `CONSOLIDACION_COSTOS` | `PasoConsolidacionCostos` | A | en main |
| 40 | `SOBREASIGNACION` | `PasoSobreasignacion` | B | en main |
| 50 | `HITOS` | — | B | reservado (B-11 / B-16) |
| 60 | `KPI` | — | B | reservado (B-12 / B-14 / B-17) |

No reordenar. COSTOS va después de las fechas de las hojas; CONSOLIDACION_COSTOS después de COSTOS.

## Qué lee y escribe cada paso

### 10 `MOTOR_FECHAS`

- **Lee:** proyecto (`fechaInicio`, `diasLaborales`), tareas hoja, dependencias entre hojas, calendario (días laborales + `no_laborable`).
- **Escribe:** `inicioPlan` y `finPlan` de las **hojas**. Sin `fechaInicio` del proyecto no hace nada.
- **No toca:** padres, línea base (`*Base`), costos, avance.
- Dependencias que tocan un padre se ignoran aquí (A-09 las rechaza al crear).

### 20 `CONSOLIDACION`

- **Lee:** árbol EDT, fechas de hojas, avances de hojas, calendario.
- **Escribe:** avance, `inicioPlan`, `finPlan` y `duracionDias` de **padres**; avance y fechas del **proyecto**.
- Avance padre = promedio de hijos ponderado por duración (si todos duran 0, promedio simple).
- Fechas padre = min inicio / max fin de hijos. Duración del padre = días hábiles entre esas fechas.

### 30 `COSTOS`

- **Lee:** asignaciones de cada hoja (`cantidad`, `cantidadReal`, `tarifa`).
- **Escribe:** `asignacion.costoPlan` = cantidad × tarifa; `costoReal` = cantidadReal × tarifa (0 si no hay real);
  `tarea.costoPlan` / `costoReal` **solo en hojas** (suma de sus asignaciones).
- **No suma padres** (eso es el 35).

### 35 `CONSOLIDACION_COSTOS`

- **Lee:** costos de las hojas ya escritos.
- **Escribe:** `costoPlan` / `costoReal` de actividades, etapas y proyecto (suma de hojas, sin duplicar).

### 40 `SOBREASIGNACION`

- **Lee:** asignaciones a profesionales, `cantidad` como horas totales, capacidad diaria del recurso,
  fechas plan de la hoja, calendario.
- **Escribe:** reconstruye `sobrecarga_alerta` (idempotente). Carga diaria = horas asignadas / días hábiles de la tarea;
  alerta si carga > capacidad. `horasSemana` y `semanas` no entran en esta fórmula (B-09 / ADR-007).
- **Lee (API):** `GET /api/proyectos/{id}/sobreasignaciones`.

### 50 `HITOS` (pendiente)

- **Debe leer:** hitos del proyecto y tareas asociadas, fechas plan/real ya consolidadas.
- **Debe escribir:** estado / fechas del hito según B-11. No marcar cumplido antes del término de sus actividades.
- Hoy no hay bean: el recálculo sigue sin este paso.

### 60 `KPI` (pendiente)

- **Debe leer:** catálogo de variables (T-02 / `docs/requerimientos.md`), definiciones de KPI, plan/costos/hitos/cambios ya persistidos.
- **Debe escribir:** snapshot en `kpi_historico` (valor, cumplimiento, semáforo, fecha). Denominador 0 → «No calculable».
- Tableros leen el histórico; no evalúan fórmulas.

## Línea base

Fijar (`POST /proyectos/{id}/linea-base`) llama a `recalcular` y **después** copia el plan vigente a `*_base`.
Un recálculo posterior **no** pisa la base. Aplicar una solicitud solo mueve el plan vigente.

## Cómo agregar un paso (B / C)

1. Constante en `OrdenRecalculo` (ya están HITOS y KPI).
2. `@Component` + `@Order` + `PasoRecalculo`.
3. Leer por `proyectoId` con repositorios; no llamar a otros módulos para recalcular.
4. Prueba de idempotencia (dos `recalcular` seguidos = mismo resultado).
