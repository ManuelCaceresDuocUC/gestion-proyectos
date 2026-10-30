# ADR-004: Alineación con la Guía de Proyecto Integrador del profesor

- **Estado:** aceptado (issue A-21). Es un cambio del propio proyecto: ver `docs/gestion/cambios.md`.
- **Contexto:** el 29-oct el profesor publicó la guía del proyecto. Al compararla con lo ya construido aparecieron diferencias de modelo y de reglas. Se corrigieron al inicio, cuando el costo es bajo.
- **Decisión:** la migración `V21__alinear_con_guia_profesor.sql` y estos cambios de código:
  - **Proyecto:** estados `PLANIFICADO`, `EN_EJECUCION`, `SUSPENDIDO`, `TERMINADO` (antes `CERRADO`). "Cerrar" deja el proyecto `TERMINADO` y en solo lectura; `SUSPENDIDO` sigue siendo editable. Nuevo `DELETE`, solo para proyectos `PLANIFICADO`.
  - **Roles:** `ADMIN` y `USUARIO_PROYECTO` (antes `VISUALIZADOR`). El usuario de proyecto trabaja proyectos, EDT, recursos, hitos y solicitudes; no administra usuarios ni KPI ni aprueba cambios.
  - **Calendario laboral:** el recálculo usa los días laborales del proyecto (`dias_laborales`) y la tabla `no_laborable` (feriados generales y por proyecto).
  - **Recursos:** columnas para especialidad, categoría, horas por semana, cantidad requerida y detalle; en asignaciones, horas por semana y semanas (costo profesional = horas/semana × semanas × valor hora).
  - **Hitos:** responsable.
  - **KPI:** ámbito (proyecto, etapa, actividad, hito, cambio), objetivo, periodicidad, fuente (automática o manual), vínculo a hito o tarea, y sentido "rango".
  - **Solicitudes de cambio:** tipos `ALCANCE, TIEMPO, COSTO, RECURSO, TECNOLOGIA, ORGANIZACION`; estados `ABIERTO, EN_EVALUACION, APROBADO, RECHAZADO, EN_IMPLEMENTACION, CERRADO` (la aprobación pasa a ser parte del estado); impacto en alcance y recursos; resultado de evaluación (efectivo, parcial, no efectivo).
- **Alternativas consideradas:** dejar los nombres anteriores y solo cambiar las etiquetas en pantalla (descartada para estados de solicitudes y roles, porque el flujo de la guía es distinto).
- **Consecuencias:** B y C deben alinear sus issues con estos valores (ver comentarios en los issues). Nada de lo ya mergeado se rompe salvo los valores de estado y rol.
