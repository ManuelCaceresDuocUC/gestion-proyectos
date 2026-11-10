# Trazabilidad: requerimientos → módulos → pruebas

Los requerimientos están en `docs/requerimientos.md`. Dev A cubre planificación; B, recursos/KPI; C, cambio, auth y tableros de cambio.
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
| RF-REC-01, RF-REC-02 Recursos y asignaciones | `recurso` (`RecursoService`, `AsignacionService`); pantallas B-10 en `frontend/src/features/recursos` | `recurso/RecursoServiceTest`, `recurso/RecursoApiTest`, `recurso/AsignacionServiceTest`, `recurso/AsignacionApiTest` |
| RF-REC-03 Costos planificados y reales | `planificacion.recalculo` (`PasoCostos`, `ConsolidadorEdt`) | `planificacion/recalculo/PasoCostosTest`, `ConsolidadorEdtTest` |
| RF-REC-04 Sobreasignación de profesionales | `planificacion.recalculo` (`PasoSobreasignacion`); lista y marca en la tarea (B-10) | `planificacion/recalculo/PasoSobreasignacionTest`, `SobrecargaAlertaApiTest`, `seed/SeedServiceTest` |
| RF-HIT-01, RF-HIT-02 Hitos y restricciones de logro | `hito` (`HitoService`); pantallas en `frontend/src/features/hitos` | `hito/HitoServiceTest`, `hito/HitoApiTest`, `hito/SemanaHitoTest`, `seed/SeedServiceTest` |
| RF-KPI-01, RF-KPI-02 Configuración y evaluación de KPI | `kpi` (`EvaluadorKpi`, `PasoKpi`) | `kpi/EvaluadorKpiTest`, `kpi/PasoKpiTest`, `kpi/CatalogoKpiProyectoTest`, `kpi/CatalogoKpiCambioTest` |
| RF-KPI-03, RF-KPI-04, RF-KPI-05 Históricos y catálogo de KPI | `kpi` (`KpiService`, `VariablesCambio`) | `kpi/PasoKpiTest`, `kpi/VariablesCambioTest`, `kpi/VariablesCambioServiceTest` |
| RF-CAM-01 Alta y ficha de solicitud | `cambio` (`SolicitudCambioService`); front C-06/C-10 | `cambio/SolicitudCambioServiceTest`, `cambio/SolicitudCambioApiTest` |
| RF-CAM-02 Filtros de solicitudes | `cambio` (listado); `features/cambios` | `cambio/SolicitudCambioServiceTest` (filtro estado/tipo/fechas), `cambio/SolicitudCambioApiTest` |
| RF-CAM-03 10 etapas y avance | `cambio` (`CambioEtapaService`, `EtapasCambio`) | `cambio/CambioEtapaServiceTest`, `cambio/CambioEtapaApiTest`, `cambio/AvanceEtapasTest`, `cambio/DiasAtrasoTest` |
| RF-CAM-04 Evidencias e historial | `cambio` (`EvidenciaService`, `SolicitudCambioService.historial`); front `TrazasCambio` | `cambio/TrazabilidadCambioApiTest`, `cambio/EvidenciaServiceTest`, `cambio/SolicitudCambioServiceTest` (escribe historial) |
| RF-CAM-05 Aprobar, rechazar y aplicar | `cambio` + `cronograma` al aplicar TIEMPO | `cambio/TransicionesSolicitudTest`, `cambio/SolicitudCambioServiceTest`, `cambio/SolicitudCambioApiTest` |
| RF-TAB-01 Tablero de proyecto | `tablero` (`TableroProyectoService`); front `features/tableros` | `tablero/TableroProyectoApiTest` |
| RF-TAB-02 Tablero de cambio | `tablero` (`TableroCambioService`); front `features/tableros` | `tablero/TableroCambioTest`, `tablero/TableroCambioApiTest` |
| RF-TAB-03 Filtros y refresco de ambos tableros | `tablero` (`FiltroTablero`); front `TablerosPagina` | `tablero/FiltroTableroTest`, `tablero/TableroCambioApiTest`, `tablero/TableroProyectoApiTest` |
| RF-INT-01 Roles ADMIN y USUARIO_PROYECTO | `auth`, `config` (JWT); login en `features/auth` | `auth/AuthApiTest`, `auth/AuthServiceTest`, `auth/JwtServiceTest` |
| RF-INT-02 Persistencia relacional | Flyway + JPA | Cubierto por A-01 y el arranque con `ddl-auto=validate` |
| RF-INT-03 Seed del caso mínimo | `seed` (`SeedService`) | `seed/SeedServiceTest`, `seed/CasoPruebaLectorTest`, `seed/SeedMapeoTest` |
| RF-INT-04 Tres recálculos de la demo | `RecalculoService` + cambio | Duración: `integracion/RecalculoDuracionE2ETest` y A-18. Recursos: pendiente B-18. Solicitud: pendiente C-14. Guion: `docs/pruebas/guion-demo.md`. Casos: `docs/pruebas/casos-de-prueba.md` (C-16) |

Cobertura documentada: R2 (proyectos y EDT), R3 (tiempos, recursos y costos), R4 (Gantt, precedencias e hitos),
R5 (KPI de proyecto/hitos y de cambio), R6 (solicitudes, etapas, evidencias, historial, aprobación y aplicar),
R7 (tableros B-21/C-12 y filtros C-13) y R8 (JWT C-01, seed, recálculo 1). Las filas marcadas como
pendientes no se presentan como cobertura automatizada.
