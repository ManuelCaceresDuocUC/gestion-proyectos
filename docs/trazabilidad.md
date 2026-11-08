# Trazabilidad: requerimientos → módulos → pruebas

Los requerimientos están en `docs/requerimientos.md`. Esta tabla cubre el núcleo de planificación (Dev A); B y C completan sus filas con sus módulos y pruebas.
Las pruebas automatizadas están en `backend/src/test/java/cl/proyecto/gestion/`.

| Requerimiento | Módulo | Pruebas automatizadas |
|---|---|---|
| RF-PRY-01, RF-PRY-02 Proyectos | `proyecto` (`ProyectoService`) | `proyecto/ProyectoServiceTest`, `proyecto/ProyectoApiTest` |
| RF-EDT-01 EDT de 3 niveles y códigos | `proyecto` (`TareaService`) | `proyecto/EdtServiceTest` |
| RF-EDT-02 Consolidación sin duplicaciones | `planificacion.recalculo` (`ConsolidadorEdt`) | `planificacion/recalculo/ConsolidadorEdtTest`, `RecalculoServiceTest` |
| RF-GAN-02 FS, SS, FF y SF con desfases | `planificacion.motor` | `planificacion/motor/MotorPlanificacionTest`, `CalendarioTest` |
| RF-GAN-03 Recálculo automático de sucesoras | `planificacion.recalculo`, `cronograma` | `planificacion/recalculo/RecalculoServiceTest`, `cronograma/CronogramaServiceTest`, `integracion/RecalculoDuracionE2ETest` |
| RF-GAN-04 Rechazo de ciclos | `planificacion.dependencia` | `dependencia/DependenciaServiceTest`, `dependencia/DependenciaApiTest`, `integracion/RecalculoDuracionE2ETest` |
| RF-GAN-05 Línea base y comparación | `planificacion.lineabase` | `lineabase/LineaBaseServiceTest`, `ComparadorPlanesTest`, `LineaBaseApiTest` |
| RF-GAN-01 Gantt jerárquico (avance, hitos, atrasos, plan/real) | frontend `features/gantt` | Prueba manual: `docs/pruebas/A-18-recalculo-duracion.md` y `scripts/demo-gantt.ps1` |
| RF-TIE-01, RF-TIE-02 Calendario, fechas y duración hábil | `planificacion.calendario`, `planificacion.motor` | `calendario/CalendarioProyectosTest`, `calendario/NoLaborableApiTest`, `motor/CalendarioTest` |
| RF-REC-01, RF-REC-02 Recursos y asignaciones | `recurso` (`RecursoService`, `AsignacionService`) | `recurso/RecursoServiceTest`, `recurso/RecursoApiTest`, `recurso/AsignacionServiceTest`, `recurso/AsignacionApiTest` |
| RF-REC-03 Costos planificados y reales | `planificacion.recalculo` (`PasoCostos`, `ConsolidadorEdt`) | `planificacion/recalculo/PasoCostosTest`, `ConsolidadorEdtTest` |
| RF-REC-04 Sobreasignación de profesionales | `recurso` y `planificacion.recalculo` | Caso de integración B-18; pendiente de prueba automatizada específica |
| RF-HIT-01, RF-HIT-02 Hitos y restricciones de logro | `hito` | Caso mínimo de `docs/pruebas/casos-de-prueba.md`; pendiente de módulo y prueba automatizada |
| RF-KPI-01, RF-KPI-02 Configuración y evaluación de KPI | `kpi` | Caso mínimo de `docs/pruebas/casos-de-prueba.md`; pendiente de módulo y prueba automatizada |
| RF-KPI-03, RF-KPI-04, RF-KPI-05 Históricos y catálogo de KPI | `kpi` | Caso mínimo de `docs/pruebas/casos-de-prueba.md`; pendiente de módulo y prueba automatizada |
| RF-INT-04 Demostración del recálculo por duración | todos los pasos del recálculo | `integracion/RecalculoDuracionE2ETest` y el caso A-18 |

Cobertura documentada: R2 (proyectos y EDT), R3 (tiempos, recursos y costos), R4 (Gantt, precedencias e hitos),
R5 (KPI), R7 (tableros) y la parte de planificación de R8. Las filas marcadas como pendientes son trabajo de
implementación posterior y no se presentan como cobertura automatizada de B-01.
