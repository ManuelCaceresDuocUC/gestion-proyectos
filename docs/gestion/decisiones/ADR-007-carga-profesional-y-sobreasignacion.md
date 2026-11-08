# ADR-007: Carga diaria y sobreasignación profesional

- **Estado:** aceptado (B-09).
- **Contexto:** las asignaciones contienen `cantidad`, `horasSemana` y `semanas`, pero B-05 define cantidad como horas o unidades y B-09 exige distribuir horas en días hábiles.
- **Decisión:** para profesionales, `asignacion.cantidad` representa las horas totales asignadas a la tarea. B-09 distribuye esas horas entre los días hábiles de `inicioPlan` a `finPlan`; `horasSemana` y `semanas` no participan en este cálculo.
- **Reglas técnicas:** se ignoran recursos sin capacidad, tareas no hoja, duración cero, fechas faltantes e intervalos sin días hábiles. La carga se calcula con escala 4 y `HALF_UP`; solo se alerta cuando carga es estrictamente mayor que capacidad.
- **Persistencia:** el paso `SOBREASIGNACION` reemplaza las alertas del proyecto en cada recálculo. El detalle se consulta mediante `GET /api/proyectos/{proyectoId}/sobreasignaciones`.
- **Seed:** P2 en 1.1.2 queda con 45 h en 5 días hábiles para producir al menos una alerta de demo que no dependa del solape accidental de tareas sin dependencias.

Esta decisión debe revisarse si el modelo de asignaciones cambia para que `horasSemana` y `semanas` sean la fuente de horas totales.
