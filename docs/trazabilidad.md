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
| RF-INT-04 Demostración del recálculo por duración | todos los pasos del recálculo | `integracion/RecalculoDuracionE2ETest` y el caso A-18 |

Cobertura de la rúbrica del núcleo: R2 (proyectos y EDT), R4 (Gantt y precedencias) y la parte de planificación de R8.
