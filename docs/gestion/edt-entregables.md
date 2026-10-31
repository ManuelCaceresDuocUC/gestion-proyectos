# Cómo armar la EDT: orientada a entregables

El profesor objetó en una entrega anterior un primer nivel como «1.1 Gestión del proyecto», porque no es un entregable.
La EDT se organiza por **lo que se entrega**, no por lo que se hace.

| Nivel | Qué es | Cómo se nombra | Ejemplo |
|---|---|---|---|
| Etapa (1) | Entregable o resultado principal | Sustantivo | Especificación de requisitos |
| Actividad (1.1) | Parte de ese entregable | Sustantivo | Catálogo de requisitos |
| Subactividad (1.1.1) | Trabajo concreto que se asigna, se mide y tiene duración | Verbo | Entrevistar a usuarios |

Reglas prácticas
- Evitar etapas como «Planificación», «Gestión del proyecto» o «Desarrollo»: son procesos, no entregables. Si hace falta, convertirlas en un entregable concreto («Plan de gestión del proyecto», «Informes de avance») o dejarlas fuera de la EDT: el seguimiento queda cubierto por los hitos y los KPI.
- Duración, responsable, recursos y dependencias se definen en las subactividades (las tareas sin hijos). Etapas y actividades consolidan fechas, avance y costos.
- Los hitos marcan la entrega de un entregable (duración 0).
- Caso mínimo de la rúbrica: 4 etapas, 12 actividades (3 por etapa) y 20 subactividades (8 actividades con 2 y 4 con 1).

## Ejemplo (4 etapas · 12 actividades · 20 subactividades)

1. **Especificación de requisitos**: 1.1 Catálogo de requisitos (Entrevistar a usuarios; Redactar requisitos) · 1.2 Modelo de datos (Diseñar modelo entidad-relación; Validar modelo con el docente) · 1.3 Plan de pruebas (Redactar casos de prueba)
2. **Diseño aprobado**: 2.1 Arquitectura del sistema (Definir la arquitectura; Documentar decisiones) · 2.2 Prototipos de interfaz (Dibujar prototipos; Revisar prototipos con usuarios) · 2.3 Diseño de base de datos (Escribir migraciones)
3. **Sistema desarrollado y probado**: 3.1 Módulo backend (Implementar la API; Implementar el motor de planificación) · 3.2 Módulo frontend (Construir las pantallas; Construir el Gantt) · 3.3 Informe de pruebas (Ejecutar pruebas integradas)
4. **Sistema entregado y documentado**: 4.1 Manual de usuario (Redactar el manual; Tomar capturas) · 4.2 Sistema desplegado (Desplegar en el servidor; Entregar al docente [hito]) · 4.3 Informe de cierre (Redactar el informe de cierre)

El script `scripts/demo-gantt.ps1` crea este proyecto por la API, con dependencias FS, SS, FF y SF, línea base y un cambio de duración.
