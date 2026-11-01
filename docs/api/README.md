# Contratos de API

Se acuerdan entre los 3 en el Sprint 0 (issue T-02) y se actualizan con cada PR que cambie un contrato.
La documentación generada está en `/swagger-ui.html` cuando el backend corre.

| Módulo | Prefijo | Dueño | Estado |
|---|---|---|---|
| auth | `/api/auth` | C | por definir |
| proyectos y tareas | `/api/proyectos`, `/api/tareas` | A | por definir |
| dependencias y recálculo | `/api/dependencias`, `/api/proyectos/{id}/recalcular` | A | por definir |
| recursos y asignaciones | `/api/recursos`, `/api/asignaciones` | B | por definir |
| hitos | `/api/hitos` | B | por definir |
| KPI | `/api/kpi` | B | por definir |
| solicitudes de cambio | `/api/proyectos/{id}/cambios`, `/api/cambios` | C | [cambios.md](cambios.md) (C-03) |
| tableros | `/api/tablero/proyecto/{id}`, `/api/tablero/cambio/{id}` | C | por definir |

Convenciones: JSON en español (`nombre`, `fechaInicio`...), fechas ISO-8601, errores como
`{ "codigo": "...", "mensaje": "..." }` con 400/403/404/409.
