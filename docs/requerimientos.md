# Requerimientos

Especificación funcional derivada de la rúbrica y de las planillas del profesor. Los issues A-02, B-01 y C-02
la completan y la cierran en el Sprint 0. Columna **Issue** = dónde se implementa; **Rúbrica** = indicador (R1-R8).

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
| RF-CAM-01 | Registrar solicitudes de cambio (alta `ABIERTO`; obligatorios nombre y tipo; responsable, prioridad, riesgo, impacto, presupuesto y costo opcionales) | R6 | C-03 |
| RF-CAM-02 | Filtrar solicitudes por estado, tipo, responsable, prioridad y rango de fechas | R6 | C-03, C-06 |
| RF-CAM-03 | Gestionar las 10 etapas del cambio con sus estados y calcular el avance | R6 | C-04, C-10 |
| RF-CAM-04 | Evidencias y trazabilidad (historial de cambios) | R6 | C-05 |
| RF-CAM-05 | Aprobar o rechazar (solo ADMIN). Aplicar es un paso posterior: si es TIEMPO dispara el recálculo; la línea base no se toca | R6, R8 | C-08, C-09 |
| RF-TAB-01 | Tablero de proyecto: avances, hitos, costos, recursos y atrasos | R7 | B-21 |
| RF-TAB-02 | Tablero de cambio: estados, riesgos, tiempos, adopción y alertas | R7 | C-12 |
| RF-TAB-03 | Filtros y actualización dinámica en ambos tableros | R7 | C-13 |
| RF-INT-01 | Dos roles: ADMIN (administra todo) y USUARIO_PROYECTO (trabaja los proyectos; no administra usuarios ni KPI ni aprueba cambios) | R8 | C-01, B-04 |
| RF-INT-02 | Persistencia en base de datos relacional | R1, R8 | A-01 |
| RF-INT-03 | Caso de prueba mínimo cargado por seed | R8 | C-07, C-11 |
| RF-INT-04 | Demostración de los 3 recálculos (duración, recursos, solicitud aprobada) | R8 | A-18, B-18, C-14, C-16 |

## Especificación B-01

Estos criterios detallan los requisitos de Dev B y son la referencia funcional para los contratos, el modelo de
datos y las pruebas de los módulos de recursos, tiempos, costos, hitos y KPI.

| ID | Prioridad | Criterio de aceptación |
|---|---|---|
| RF-TIE-01 | Alta | Registrar fechas planificadas, de línea base y reales, y calcular duración en días hábiles. |
| RF-TIE-02 | Alta | Aplicar días laborales del proyecto y excluir feriados generales y días no laborables propios. |
| RF-REC-01 | Alta | Mantener cuatro tipos de recurso; cada recurso debe tener tarifa y cada profesional capacidad diaria. |
| RF-REC-02 | Alta | Asignar recursos a tareas hoja con cantidad y tarifa, rechazando duplicados y recursos inactivos. |
| RF-REC-03 | Alta | Calcular `costo plan = cantidad x tarifa` y `costo real = cantidadReal x tarifa`, y consolidarlos sin duplicación. |
| RF-REC-04 | Alta | Calcular `carga diaria = horas asignadas / días hábiles de la tarea` y alertar cuando `carga diaria > capacidad diaria`. |
| RF-HIT-01 | Media | Registrar un hito de duración cero con todos los campos indicados en la matriz de hito siguiente. |
| RF-HIT-02 | Alta | Impedir que un hito se marque cumplido antes del término de sus actividades asociadas. |
| RF-KPI-01 | Alta | Configurar variables, fórmula, unidad, meta, operador, sentido, umbrales, periodicidad y responsable. |
| RF-KPI-02 | Alta | Devolver `No calculable` con denominador cero y aplicar semáforo Verde >= 100%, Amarillo 80-99%, Rojo < 80%. |
| RF-KPI-03 | Media | Guardar un snapshot histórico en cada recálculo y generar alertas cuando no se alcance la meta. |
| RF-KPI-04/05 | Media | Cargar ocho KPI de proyecto e hitos y al menos doce KPI de gestión del cambio. |

### Recursos, asignaciones y costos

- Deben existir cuatro tipos de recurso en el caso mínimo. Cada recurso tiene nombre, tipo, tarifa vigente y estado
  activo; un profesional además declara capacidad diaria.
- La tarifa y la cantidad de una asignación no pueden ser negativas y una asignación solo puede apuntar a una tarea
  hoja del mismo proyecto. Una tarea no puede repetir el mismo recurso.
- La asignación conserva la tarifa aplicada para no alterar el historial cuando cambie la tarifa del recurso.
- El costo planificado de una asignación es `cantidad x tarifa`; el costo real es `cantidadReal x tarifa`. El
  recálculo persiste ambos valores y consolida las tareas hoja hacia la EDT y el proyecto sin duplicarlos.
- Una modificación de asignación debe disparar el recálculo del proyecto. Un proyecto terminado no admite cambios.
- La carga diaria de un profesional se calcula como `horas asignadas / días hábiles de la tarea`. Si la carga diaria
  es mayor que la capacidad diaria del profesional, se genera una alerta de sobreasignación.
- En B-09, las horas asignadas son `asignacion.cantidad`; `horasSemana` y `semanas` no intervienen en esta alerta.

### Tiempos y calendario laboral

- Las fechas planificadas, de línea base y reales se mantienen separadas. La línea base solo cambia al fijar una nueva
  versión y el recálculo no la modifica.
- La duración se expresa en días hábiles según los días laborales del proyecto, excluyendo feriados generales y
  días no laborables propios.
- Un día no laborable se identifica por proyecto y fecha; la misma fecha no se puede registrar dos veces en el mismo
  ámbito.

### Hitos

- Un hito representa un logro verificable y tiene duración cero. Sus campos son: logro, actividades asociadas,
  semana prevista, fecha planificada, fecha de línea base, fecha real opcional, estado, evidencia, indicador,
  unidad, criterio de aceptación, validador y acción si no se cumple.
- Cada hito pertenece a un proyecto y puede asociarse a una o más actividades. No se puede marcar como cumplido sin
  fecha real ni antes de que terminen sus actividades asociadas.
- El caso mínimo debe contener seis hitos y cubrir estados pendientes, cumplidos y no cumplidos.

### KPI

- Un KPI configurable declara código, categoría, ámbito, fórmula, variables y origen, unidad, operador, sentido,
  meta, umbrales, periodicidad y responsable.
- Los operadores de meta admitidos son `>=`, `<=`, `>`, `<` y `entre límites`; el sentido indica si un valor mayor o
  menor representa mejor cumplimiento.
- Las fórmulas deben usar variables de la misma población y período. Un denominador cero produce el resultado
  `No calculable`, sin dividir ni asignar un semáforo falso.
- El cumplimiento se expresa como porcentaje y el semáforo es Verde (>= 100%), Amarillo (80-99%) o Rojo (< 80%),
  según el sentido y la meta configurados. Solo se redondea el resultado final a dos decimales.
- Cada recálculo guarda un histórico inmutable con valor, cumplimiento, semáforo y fecha de cálculo, y puede generar
  una alerta cuando no se alcanza la meta.
- El caso mínimo debe incluir ocho KPI de proyecto e hitos y al menos doce KPI de gestión del cambio.

### Campos de hito

La ficha de cada hito debe conservar los campos de `Actividad_Gantt_Hitos.xlsx`: logro verificable, actividades
asociadas, semana prevista, evidencia, indicador o cálculo, unidad, criterio de aceptación, quién valida y acción si
no se cumple. Las fechas planificada, de línea base y real, junto con el estado, permiten comprobar el cumplimiento
sin confundir el hito con una tarea de duración positiva.

## Especificación C-02

Estos criterios detallan la gestión del cambio (R6). Son la referencia funcional para el contrato
`docs/api/cambios.md` y para las pruebas del módulo. Los tableros, los filtros y el guion preliminar
de los tres recálculos completan este issue (R7 y R8).

Los nombres de la planilla del profesor se transcriben abajo. En el sistema rigen los catálogos de
**V21 / ADR-004**. No hay estado Suspendido. Cancelado de la planilla se cubre con `RECHAZADO`.

| ID | Prioridad | Criterio de aceptación |
|---|---|---|
| RF-CAM-01 | Alta | Registrar solicitud. Obligatorios: `nombre` y `tipo`. El alta queda `ABIERTO`, con código `SC-nn` y las 10 etapas en `PENDIENTE`. Responsable, prioridad, riesgo, impacto, presupuesto y costo son opcionales (el servidor pone prioridad/riesgo/impacto medios e impactos 0 si faltan). |
| RF-CAM-02 | Alta | Filtrar el listado por `estado`, `tipo`, `prioridad`, `responsableId` y rango de `fechaSolicitud`. El riesgo se guarda en la ficha; no es query del listado (sí lo será del tablero, C-13). |
| RF-CAM-03 | Alta | Las 10 etapas de la plantilla, cada una con estado de etapa; `avance` (0–100) = etapas `COMPLETADO` × 10. |
| RF-CAM-04 | Media | Conservar evidencias (descripción y URL; sin archivos) e historial inmutable por solicitud (C-05). Cada cambio real de estado de solicitud o de etapa deja fila en `cambio_historial`; `usuarioId` / `creadoPor` salen del JWT. |
| RF-CAM-05 | Alta | Aprobar y rechazar son un acto de **ADMIN** distinto de aplicar al plan. Ver más abajo. |

### Las 10 etapas (plantilla)

Al crear la solicitud se insertan siempre estas diez, en este orden, estado inicial `PENDIENTE`.
El cliente no las envía. No hay secuencia obligatoria entre etapas: cualquiera puede pasar a
cualquiera de los cinco estados de etapa.

| Nº | Nombre (plantilla) |
|---|---|
| 1 | Necesidad |
| 2 | Evaluación |
| 3 | Riesgos |
| 4 | Planificación |
| 5 | Comunicación inicial |
| 6 | Implementación |
| 7 | Seguimiento |
| 8 | Resultados corto plazo |
| 9 | Correctivas |
| 10 | Adopción cultural |

**Adoptado** en la plantilla no es un estado de la solicitud: es la etapa 10 en `COMPLETADO`
(consolidación cultural). **Cerrado** sí es estado de la solicitud (`CERRADO`): formalmente finalizado.

### Estado de etapa (plantilla = sistema)

| Plantilla | Sistema |
|---|---|
| Pendiente | `PENDIENTE` |
| En curso | `EN_CURSO` |
| Completado | `COMPLETADO` |
| Bloqueado | `BLOQUEADO` |
| No aplica | `NO_APLICA` |

- `avance` = etapas `COMPLETADO` × 10 (es decir, completadas / 10 × 100). `NO_APLICA` no suma.
- `etapasBloqueadas` = cuántas están `BLOQUEADO`.
- Días de atraso (calculado, no persistido), en días corridos y mínimo 0: con `finReal` →
  `finReal − finPrevisto`; sin `finReal` y estado `CERRADO` o `RECHAZADO` → 0; sin `finReal` en
  cualquier otro estado → `hoy − finPrevisto`; sin `finPrevisto` → 0.

### Estado de la solicitud: plantilla → V21

La plantilla trae: Abierto, En curso, Cerrado, Suspendido, Cancelado.
La guía del profesor (ADR-004) pide un flujo con evaluación, aprobación e implementación.

| Plantilla | Sistema (V21) | Notas |
|---|---|---|
| Abierto | `ABIERTO` | Alta. Aún no se evalúa. |
| En curso | `EN_EVALUACION`, `APROBADO`, `EN_IMPLEMENTACION` | La guía parte “En curso” en evaluación, decisión y ejecución. |
| Cerrado | `CERRADO` | Formalmente finalizado. Terminal. |
| Cancelado | `RECHAZADO` | No se aplica al plan. Terminal. |
| Suspendido | — | No existe en V21; no se implementa. |

Transiciones válidas (C-08):

```
ABIERTO            → EN_EVALUACION
EN_EVALUACION      → ABIERTO | APROBADO | RECHAZADO
APROBADO           → EN_IMPLEMENTACION
EN_IMPLEMENTACION  → CERRADO
RECHAZADO, CERRADO → (terminales)
```

Un salto fuera del grafo es error de negocio (`TRANSICION_INVALIDA`). El proyecto `TERMINADO`
no admite altas ni cambios de solicitud.

### Qué significa aprobar (y qué no)

**Aprobar** (`POST /api/cambios/{id}/aprobar`, o PUT a `APROBADO` desde `EN_EVALUACION`):

- Solo **ADMIN**. Un `USUARIO_PROYECTO` recibe 403.
- Solo desde `EN_EVALUACION`.
- Pasa a `APROBADO` y llena `aprobadaEn` la primera vez.
- **No** marca `aplicada`. **No** mueve el plan, ni costos, ni KPI. **No** toca la línea base.

**Rechazar** (mismo privilegio, desde `EN_EVALUACION`): pasa a `RECHAZADO`. No llena `aprobadaEn`.
No mueve el plan.

**Aplicar al plan** (`POST /api/cambios/{id}/aplicar`) es el paso que marca `aplicada` y, si el tipo
es `TIEMPO`, dispara el recálculo. Lo puede un usuario autenticado (no hace falta ADMIN). Solo si
está `APROBADO` y `aplicada = false`.

| Tipo | Efecto en el plan | Costos | KPI | Línea base |
|---|---|---|---|---|
| `TIEMPO` | Suma `impactoDias` a la duración de la tarea hoja (`tareaId`) por `CronogramaService.parchear`. Ese servicio llama a `RecalculoService.recalcular`. No se escriben fechas a mano. | El recálculo vuelve a calcular costos plan/real (paso de B) y los consolida. | El recálculo toma snapshot y reevalúa si el paso de KPI está implementado (B-14, B-17). | No cambia. |
| `ALCANCE`, `COSTO`, `RECURSO`, `TECNOLOGIA`, `ORGANIZACION` | No mueven el Gantt. No llaman a `RecalculoService`. Se marcan aplicadas y pasan a `EN_IMPLEMENTACION`. | Sin recálculo: costos del plan no cambian. | Sin recálculo: KPI no se reevalúan. | No cambia. |

Tras aplicar con éxito: `aplicada = true`, `aplicadaEn` la primera vez, estado `EN_IMPLEMENTACION`.
Un segundo aplicar responde conflicto (`YA_APLICADA`) y no vuelve a sumar días.
Si el TIEMPO no se puede aplicar (sin tarea hoja, duración negativa), no se marca `aplicada`
ni se cambia el estado.

### Tableros de comando (R7)

Los tableros **solo leen**. No recalculan: muestran lo que ya persistió `RecalculoService`.
Son dos pantallas distintas (no un único dashboard). Datos: `GET /api/tablero/proyecto/{id}` (B-21)
y `GET /api/tablero/cambio/{id}` (C-12). El layout compartido vive en `features/tableros`.

| ID | Prioridad | Criterio de aceptación |
|---|---|---|
| RF-TAB-01 | Alta | Tablero de proyecto con ejecución, avances, hitos, presupuesto, recursos y atrasos, más KPI de proyecto/hitos con semáforo. |
| RF-TAB-02 | Alta | Tablero de cambio con distribución por estado, riesgos, tiempos, costos, etapas, adopción, efectividad y alertas. |
| RF-TAB-03 | Alta | Filtros abajo. Tras un recálculo o un cambio de solicitud, ambos se refrescan sin recargar la página (re-consulta al volver o polling corto). |

**Tablero de proyecto** (B-21; guía §5: distinto del de cambio):

- Avance real vs planificado.
- Hitos: cumplidos, próximos y vencidos.
- Costos: plan, real y presupuesto.
- Uso de recursos y alertas de sobreasignación.
- Actividades atrasadas.
- KPI de proyecto e hitos con semáforo (Verde / Amarillo / Rojo / No calculable).
- Justificación visible de por qué se eligieron esos KPI (cuando B-13/B-15 existan).

**Tablero de cambio** (C-12; hoja «Tablero» de la plantilla + comentario ADR-004 en #102):

- Conteos: registrados, en evaluación / implementación («en curso»), cerrados, rechazados, atrasados.
- Avance promedio de las 10 etapas; cumplimiento de etapas; KPI de cambio en verde.
- Distribución por estado (catálogo V21).
- Riesgos (críticos y altos) y costos (presupuesto previsto vs costo real de las cerradas).
- Tiempos de implementación y días de atraso.
- Acciones correctivas (etapa 9 o campo `accionCorrectiva`).
- Efectividad (`resultadoEvaluacion`: efectivo, parcial, no efectivo).
- Adopción: solicitudes con etapa 10 `COMPLETADO`.
- Alertas: atrasados, desviaciones, riesgos críticos, etapas bloqueadas, KPI en rojo / meta incumplida.

Dependencias de implementación: el tablero de proyecto pide B-14 y B-16; el de cambio pide B-17
para los KPI. C-02 fija el *qué*; B-21 y C-12 el *cómo*.

### Filtros (C-13)

| Tablero | Filtros (guía / #104) |
|---|---|
| Proyecto | Proyecto, etapa de la EDT, rango de fechas, responsable |
| Cambio | Estado, prioridad, riesgo, responsable |

Un cambio de filtro reconsulta el endpoint; no se calcula en el cliente lo que el servidor ya agregó.

### Guion preliminar de los 3 recálculos (RF-INT-04)

Guion corto para C-02. El guion cronometrado y quién habla minuto a minuto es **C-16**
(`docs/pruebas/guion-demo.md`). Entrar como ADMIN. Caso: proyecto seed `EEC-2026`.
Tras cada recálculo se muestran **Gantt, costos, KPI y los dos tableros**, y se explica por qué
cambiaron (R8). La línea base no se mueve en ninguno de los tres.

| # | Qué se hace | Efecto esperado | Issue de prueba |
|---|---|---|---|
| 1 | En el Gantt, cambiar la duración de una tarea hoja (p. ej. +2 días) y recalcular. | Sucesoras FS/FF se corren; consolidación de fechas; costos de esa rama si hay asignaciones; KPI de atraso/avance; tablero de proyecto refleja atrasos. | A-18 |
| 2 | Modificar una asignación (cantidad o recurso) de un profesional. | Costo plan/real de la hoja y del proyecto; alerta de sobreasignación si carga diaria > capacidad (en el seed, P2 en 1.1.2 ya dispara una); KPI de recursos; tablero de proyecto. | B-18 |
| 3 | En una solicitud **tipo `TIEMPO`** con `tareaId` e `impactoDias`: Enviar a evaluación → **Aprobar** (ADMIN) → **Aplicar al plan**. | Aprobar no mueve nada. Aplicar suma los días, pasa a `EN_IMPLEMENTACION`, `aplicada=true`. Gantt y costos del recálculo; KPI de cambio (B-17) y tablero de cambio (conteos, atraso, adopción). Línea base igual que antes. | C-14 |

Notas para el recálculo 3:

- Aprobar y aplicar son dos clics distintos (especificación de RF-CAM-05).
- Las 5 solicitudes del seed C-11 nacen `ABIERTO` y hoy son `TECNOLOGIA` u `ORGANIZACION`:
  aplicarlas **no mueve el Gantt**. Para la demo hace falta una `TIEMPO` (alta en caliente o
  ajuste posterior del seed). Si se aplica un tipo que no es TIEMPO, se muestra el aviso
  de C-10 y se explica que el plan no cambia.
- Después, entrar como `USUARIO_PROYECTO`: puede editar y aplicar; no ve Aprobar/Rechazar.

## Requerimientos no funcionales

- Un único punto de recálculo (`RecalculoService`); ningún módulo recalcula por su cuenta.
- Migraciones versionadas con Flyway; nunca se edita una migración ya publicada.
- Seed reproducible en menos de un minuto (`scripts/reset-demo.sh`).
- Tiempo de recálculo del caso de prueba menor a 2 s.
- Entorno desplegado accesible para los 3 desarrolladores y para la demo.

## Reglas de negocio tomadas de las planillas del profesor

**Gestión del cambio** (`Plantilla_Seguimiento_Gestion_del_Cambio_KPI.xlsx`)
- 10 etapas y estados de etapa: transcritos en la especificación C-02. El sistema usa esos nombres.
- Estado del cambio en la planilla: Abierto, En curso, Cerrado, Suspendido, Cancelado.
  "Cerrado" = formalmente finalizado; "Adoptado" = etapa 10 completada (consolidación cultural).
  El flujo implementado es el de V21 (especificación C-02); no hay estado Suspendido.
- Días de atraso y avance: reglas C-02 (atraso en días corridos; `RECHAZADO` cuenta como cerrado para el atraso).
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

## Catálogo de variables para fórmulas de KPI (acordado en T-02)

Contrato: `docs/api/kpi-variables.md`. No usar nombres fuera de esa lista.

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

### Origen de las variables de proyecto e hitos (B-13)

Ninguna se ingresa a mano. `proyecto.avance` está en puntos 0–100 y entra a la fórmula como razón (`avance / 100`); lo mismo `AVANCE_PLAN`. Si la unidad del KPI es `%`, `EvaluadorKpi` multiplica esa razón por 100 una sola vez.

- `AVANCE_REAL`: `proyecto.avance / 100`.
- `AVANCE_PLAN`: avance planificado de cada hoja a la fecha del recálculo (100 si `finPlan` ya pasó, 0 si aún no empieza, si no los días hábiles transcurridos sobre la duración). Se sube por el árbol como la consolidación: promedio ponderado por duración y, si todos los pesos son 0, promedio simple.
- `VALOR_GANADO`: suma del costo plan de cada hoja por su avance real. `VALOR_PLANIFICADO` y `COSTO_PLAN_A_LA_FECHA`: la misma suma usando la fracción planificada. `COSTO_REAL`: `proyecto.costoReal`.
- `DIAS_ATRASO_PROYECTO`: sin línea base, 0. Con `fechaFinBase` y sin `fechaFinPlan`, la variable no se calcula y KP-04 queda «No calculable». Con ambas fechas, días hábiles de la base al fin plan (la misma cuenta que la comparación de línea base), mínimo 0.
- `ACTIVIDADES_TOTAL` y `ACTIVIDADES_ATRASADAS`: hojas, es decir nodos sin hijos, sea cual sea su tipo. Atrasada: `finPlan` anterior a hoy y avance menor a 100.
- `HITOS_VENCIDOS`: `fechaPlan` anterior a hoy. `HITOS_CUMPLIDOS_A_TIEMPO`: de esos, estado `CUMPLIDO` con `fechaReal` no posterior a `fechaPlan`.
- `PROFESIONALES_TOTAL`: recursos distintos asignados al proyecto cuyo tipo se llama `Profesional`. `PROFESIONALES_SOBREASIGNADOS`: de esos, los que tienen al menos una fila en `sobreasignacion_alerta` (la tabla guarda una fila por profesional y fecha; el conteo es de personas).

KP-01 guarda `meta` nula. Al evaluar, la meta efectiva es `AVANCE_PLAN × 100`, porque la comparación del evaluador ocurre en puntos porcentuales. Los ocho KPI son los de `db/seed/kpi-definiciones.json` (`KP-01`…`KP-08`). El paso de recálculo los inserta como definiciones globales si faltan. No inserta los de cambio.

### Histórico y alertas (B-14)

Cada recálculo agrega una fila en `kpi_historico` por KPI activo: valor, cumplimiento, semáforo y motivo `RECALCULO`. Valor nulo significa «No calculable». Hay alerta si el semáforo es rojo o si el cumplimiento es menor que el del snapshot anterior. El primero solo alerta en rojo. `GET /api/proyectos/{id}/kpi` devuelve el último snapshot y la alerta. `GET /api/kpi/{id}/historico?proyectoId=` devuelve la serie, más reciente primero.
