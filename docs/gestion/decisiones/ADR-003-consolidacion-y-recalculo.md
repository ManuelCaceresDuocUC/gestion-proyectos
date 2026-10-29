# ADR-003: Consolidación hacia arriba y orden del recálculo

- **Estado:** aceptado (A-06, A-12)
- **Contexto:** al cambiar una duración, un recurso o una solicitud aprobada, todo el proyecto debe quedar consistente (fechas, avance, costos). Hay un único punto de entrada: `RecalculoService.recalcular(proyectoId)`.
- **Decisión:**
  - **Avance** de un padre = promedio de sus hijos ponderado por duración en días hábiles. Si todos los hijos duran 0 (hitos), promedio simple. El avance del proyecto se calcula igual sobre sus etapas.
  - **Fechas** de un padre: inicio = mínimo y fin = máximo de sus hijos. Su duración = días hábiles entre ambas (se pisa lo que se haya escrito a mano, porque se deriva).
  - **Costos**: solo las hojas tienen costo propio (lo calcula el paso COSTOS de B). Los padres y el proyecto suman a sus hijos, por lo que no hay doble conteo y repetir el cálculo da el mismo resultado.
  - **Dependencias** solo entre tareas hoja. Una dependencia que toque un padre se ignora en el cálculo (A-09 la rechazará al crearla).
  - Un proyecto sin fecha de inicio no calcula fechas.
  - Orden de pasos: `MOTOR_FECHAS` (10) → `CONSOLIDACION` (20, avance y fechas) → `COSTOS` (30, hojas) → `CONSOLIDACION_COSTOS` (35, suma de costos hacia arriba) → `SOBREASIGNACION` (40) → `HITOS` (50) → `KPI` (60). Se necesitó un segundo paso de consolidación porque los costos de las hojas se calculan después de las fechas.
  - Si un paso falla, la transacción completa se revierte y el error trae código y mensaje (ciclo: 409 `CICLO_EN_DEPENDENCIAS`; otro fallo: 500 `RECALCULO_FALLIDO`).
  - Los servicios que modifican la EDT o la fecha de inicio del proyecto llaman a `recalcular` al terminar.
- **Alternativas consideradas:** consolidar costos en el mismo paso que las fechas (descartada: quedarían un recálculo atrasados); recalcular solo lo afectado (descartada: más complejo, y el proyecto es chico).
- **Consecuencias:** el recálculo completo es simple y predecible; los módulos de B y C solo deben registrar su `PasoRecalculo` con el orden de `OrdenRecalculo`.
