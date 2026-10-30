# Requerimientos

Borrador inicial derivado de la rúbrica y de las planillas del profesor. Los issues A-02, B-01 y C-02
lo completan y lo cierran en el Sprint 0. Columna **Issue** = dónde se implementa; **Rúbrica** = indicador (R1-R8).

## Requerimientos funcionales

| ID | Requerimiento | Rúbrica | Issue |
|---|---|---|---|
| RF-PRY-01 | Crear, editar, consultar y cerrar proyectos | R2 | A-04, A-10 |
| RF-PRY-02 | Gestionar estado, responsable, presupuesto y avance del proyecto | R2 | A-04 |
| RF-EDT-01 | EDT de 3 niveles: etapas, actividades y subactividades, con códigos automáticos (1, 1.1, 1.1.1) | R2 | A-05, A-11 |
| RF-EDT-02 | Consolidar avance, fechas y costos hacia las etapas y el proyecto, sin duplicaciones | R2 | A-06 |
| RF-TIE-01 | Registrar fechas planificadas, de línea base y reales; calcular duración en días hábiles | R3 | B-07 |
| RF-TIE-02 | Calendario laboral con días no laborables | R3 | B-07 |
| RF-REC-01 | Tipos de recurso (4) y recursos con tarifa; profesionales con capacidad diaria | R3 | B-05 |
| RF-REC-02 | Asignar recursos a tareas con cantidad y tarifa | R3 | B-06 |
| RF-REC-03 | Calcular costo planificado y real por actividad y consolidarlo | R3 | B-08 |
| RF-REC-04 | Alertar la sobreasignación de profesionales | R3 | B-09 |
| RF-GAN-01 | Carta Gantt jerárquica con avance, hitos, atrasos y comparación plan/real | R4 | A-15, A-16 |
| RF-GAN-02 | Dependencias FS, SS, FF y SF con desfases (positivos y negativos) | R4 | A-07, A-08 |
| RF-GAN-03 | Recalcular automáticamente las sucesoras al cambiar duración, fechas o dependencias | R4, R8 | A-12, A-13, A-17 |
| RF-GAN-04 | Rechazar ciclos de dependencias | R4 | A-09 |
| RF-GAN-05 | Línea base y comparación plan vs real | R4 | A-14 |
| RF-HIT-01 | Hitos de duración cero con logro, actividades asociadas, evidencia, indicador, criterio, validador y acción si no se cumple | R4 | B-11 |
| RF-HIT-02 | Un hito no puede ocurrir antes del término de las actividades de las que depende | R4 | B-11 |
| RF-KPI-01 | Administrador de KPI configurable: variables, fórmula, meta, operador, sentido, umbrales y responsable | R5 | B-12, B-15 |
| RF-KPI-02 | Resultado "No calculable" ante denominador cero; semáforo Verde (>=100%), Amarillo (80-99%), Rojo (<80%) | R5 | B-12 |
| RF-KPI-03 | Históricos de cada KPI (snapshot en cada recálculo) y alertas | R5 | B-14 |
| RF-KPI-04 | 8 KPI de proyecto e hitos | R5 | B-13 |
| RF-KPI-05 | KPI de gestión del cambio: 10 de la plantilla + adicionales (mínimo 12 en total) | R5 | B-17 |
| RF-CAM-01 | Registrar solicitudes de cambio con responsable, estado, prioridad, riesgo, impacto, presupuesto y costo | R6 | C-03 |
| RF-CAM-02 | Filtrar solicitudes por estado, responsable, prioridad, riesgo y fechas | R6 | C-03, C-06 |
| RF-CAM-03 | Gestionar las 10 etapas del cambio con sus estados y calcular el avance | R6 | C-04, C-10 |
| RF-CAM-04 | Evidencias y trazabilidad (historial de cambios) | R6 | C-05 |
| RF-CAM-05 | Aprobar o rechazar solicitudes; la aprobada se aplica al plan, costos y KPI y conserva la línea base | R6, R8 | C-08, C-09 |
| RF-TAB-01 | Tablero de proyecto: avances, hitos, costos, recursos y atrasos | R7 | B-21 |
| RF-TAB-02 | Tablero de cambio: estados, riesgos, tiempos, adopción y alertas | R7 | C-12 |
| RF-TAB-03 | Filtros y actualización dinámica en ambos tableros | R7 | C-13 |
| RF-INT-01 | Dos roles: ADMIN (administra todo) y USUARIO_PROYECTO (trabaja los proyectos; no administra usuarios ni KPI ni aprueba cambios) | R8 | C-01, B-04 |
| RF-INT-02 | Persistencia en base de datos relacional | R1, R8 | A-01 |
| RF-INT-03 | Caso de prueba mínimo cargado por seed | R8 | C-07, C-11 |
| RF-INT-04 | Demostración de los 3 recálculos (duración, recursos, solicitud aprobada) | R8 | A-18, B-18, C-14, C-16 |

## Requerimientos no funcionales

- Un único punto de recálculo (`RecalculoService`); ningún módulo recalcula por su cuenta.
- Migraciones versionadas con Flyway; nunca se edita una migración ya publicada.
- Seed reproducible en menos de un minuto (`scripts/reset-demo.sh`).
- Tiempo de recálculo del caso de prueba menor a 2 s.
- Entorno desplegado accesible para los 3 desarrolladores y para la demo.

## Reglas de negocio tomadas de las planillas del profesor

**Gestión del cambio** (`Plantilla_Seguimiento_Gestion_del_Cambio_KPI.xlsx`)
- 10 etapas: 1 Necesidad, 2 Evaluación, 3 Riesgos, 4 Planificación, 5 Comunicación inicial, 6 Implementación,
  7 Seguimiento, 8 Resultados corto plazo, 9 Correctivas, 10 Adopción cultural.
- Estado del cambio: Abierto, En curso, Cerrado, Suspendido, Cancelado. Estado de etapa: Pendiente, En curso,
  Completado, Bloqueado, No aplica. "Cerrado" = formalmente finalizado; "Adoptado" = consolidación cultural.
- Días de atraso: si hay fin real, max(0, fin real - fin previsto); si no y no está cerrado, max(0, hoy - fin previsto); si no, 0.
- Avance de un cambio = etapas completadas / 10.
- Cumplimiento de KPI: mayor es mejor = valor/meta; menor es mejor = meta/valor (1 si valor = 0), con tope 1.
  Semáforo: Verde >= 100%, Amarillo 80-99%, Rojo < 80%. Las metas son editables.

**KPI** (`Actividad_30_KPI.xlsx`)
- Cada KPI declara: fórmula, variables y su origen, unidad, condición o meta (>=, <=, <, >, entre límites).
- Si el denominador es cero: "No calculable". Se redondea solo el resultado final (2 decimales).
- Usar población y período coincidentes entre numerador y denominador.

**Hitos** (`Actividad_Gantt_Hitos.xlsx`)
- Un hito es un logro verificable sin duración (no una tarea). Se asocia a actividades, tiene semana prevista, evidencia,
  indicador/cálculo, unidad y criterio de aceptación, quién valida y qué hacer si no se cumple.
- No se marca "logrado" sin datos reales; no puede ocurrir antes de que termine la actividad de la que depende.

## Catálogo de variables para fórmulas de KPI (a acordar en T-02)

| Variable | Significado |
|---|---|
| AVANCE_REAL, AVANCE_PLAN | % de avance físico real y planificado a la fecha |
| VALOR_PLANIFICADO, VALOR_GANADO | PV y EV (costo plan x % planificado / % real) |
| COSTO_REAL, COSTO_PLAN_A_LA_FECHA | AC y costo plan acumulado a la fecha |
| DIAS_ATRASO_PROYECTO | Fin plan actual - fin de línea base (días) |
| ACTIVIDADES_TOTAL, ACTIVIDADES_ATRASADAS | Tareas hoja y las vencidas con avance < 100% |
| HITOS_VENCIDOS, HITOS_CUMPLIDOS_A_TIEMPO | Hitos con fecha vencida y los cumplidos en plazo |
| PROFESIONALES_TOTAL, PROFESIONALES_SOBREASIGNADOS | Profesionales y los que superan su capacidad diaria |
| CAMBIOS_TOTAL, CAMBIOS_CERRADOS, CAMBIOS_CERRADOS_EN_PLAZO | Solicitudes y sus estados |
| CAMBIOS_RIESGO_ALTO, CAMBIOS_PRIORIDAD_ALTA, CAMBIOS_CON_ATRASO, CAMBIOS_CON_BLOQUEO, CAMBIOS_CON_CORRECTIVA | Conteos según planilla |
| CAMBIOS_CERRADOS_CON_SOBRECOSTO, CAMBIOS_CERRADOS_CON_PRESUPUESTO | KC-05 |
| ETAPAS_COMPLETADAS, ETAPAS_TOTALES | KC-06 (totales = 10 x cambios seguidos) |
| CAMBIOS_ADOPTADOS | Cambios con la etapa 10 completada |
| COSTO_REAL_CAMBIOS_CERRADOS, PRESUPUESTO_CAMBIOS_CERRADOS, DIAS_ATRASO_CAMBIOS | KC-11, KB-22 |
| CAMBIOS_APROBADOS, CAMBIOS_APROBADOS_APLICADOS | KC-13 |
