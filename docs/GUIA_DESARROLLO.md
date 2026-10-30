# Guía de desarrollo
### Proyecto integrador: sistema de planificación, seguimiento y gestión del cambio

> Léela completa una vez (10 minutos) y luego usa la sección **"Tus issues"**. Todo lo que está aquí
> se creó de forma automática junto con el repositorio, el tablero de GitHub Project y los issues.

## 1. Introducción: el caso

El ramo es **Gestión de Proyectos de Software**. Lo que se evalúa no es solo que el programa funcione:
al profesor le interesa que **dominemos la gestión**. Por eso construimos, en equipo de 3 y de forma remota,
un software que sirve para **iniciar y gestionar proyectos**: se crea el proyecto, se descompone en una EDT,
se programa con dependencias, se asignan recursos y costos, se mide con KPI, se controlan los cambios y se
sigue todo en dos tableros de comando.

Hay una segunda lectura, igual de importante: **nosotros mismos gestionamos el desarrollo como un proyecto**.
Usamos GitHub Project (EDT, sprints, responsables, estimaciones, hitos, riesgos y control de cambios) y dejamos
la evidencia en `docs/gestion/`. Eso es lo que el profesor verá en la gestión, además del producto.

- **Entrega:** jueves **5 de noviembre de 2026** (aplicación, documentación técnica y demostración de un caso de prueba).
- **Modalidad:** evaluación grupal de una aplicación funcional e integrada.
- **Caso de prueba:** "Habilitar un espacio de estudio colaborativo" (12 semanas), tomado de la planilla
  `Actividad_Gantt_Hitos.xlsx` y adaptado a la estructura mínima que exige la rúbrica (ver `db/seed/`).

## 2. Qué pide la evaluación

La nota es 100% el producto integrado: **cada indicador pesa según la tabla y el puntaje es ponderación x % de logro**.
Niveles de logro: Muy buen desempeño 100%, Buen desempeño 80%, Aceptable 60%, Incipiente 30%, No logrado 0%.

| # | Indicador | Peso | Qué debe poder demostrarse |
|---|---|---|---|
| R1 | Requerimientos y modelo de datos | 5% | Requerimientos de todos los módulos; modelo coherente (proyectos, EDT, precedencias, recursos, hitos, KPI, cambios) con relaciones, persistencia y reglas de integridad verificables |
| R2 | Administración de proyectos y EDT | 5% | Crear, editar, consultar y cerrar proyectos (estado, responsable, presupuesto, avance); EDT de etapas, actividades y subactividades con códigos y consolidación sin duplicaciones |
| R3 | Recursos, tiempos y costos | 10% | Fechas planificadas y reales, calendario, recursos asignados; duración, costos plan y real por actividad con tarifas y cantidades; alerta de sobreasignación profesional |
| R4 | Carta Gantt y precedencias | 10% | Gantt dinámico y jerárquico con avance, hitos, atrasos y plan/real; FS, SS, FF y SF con desfases; recálculo automático de sucesoras; rechazo de ciclos |
| R5 | Construcción y cálculo de KPI | 10% | Administrador configurable (variables, fórmulas, metas, umbrales, históricos, alertas); KPI de proyecto, hitos y cambios (incluye los adicionales de cambio) |
| R6 | Gestión del cambio | 5% | Registrar y filtrar solicitudes (responsables, estados, impactos, evidencias); 10 etapas; trazabilidad y línea base; cambios aprobados reflejados en planificación, costos y KPI |
| R7 | Dos tableros de comando | 5% | Tablero de proyecto (avances, hitos, costos, recursos, atrasos) y de cambio (estados, riesgos, tiempos, adopción, alertas), con filtros y actualización dinámica |
| R8 | Integración, pruebas y demostración | **50%** | Aplicación integrada y persistente con **2 roles**; documentación y caso mínimo completo; demostración de recálculos por **cambio de duración, cambio de recursos y solicitud aprobada**, explicando sus efectos |

**Caso mínimo que se verificará:** 4 etapas, 12 actividades, 20 subactividades, 4 tipos de dependencia, 5 profesionales,
4 tipos de recurso, 6 hitos, 8 KPI de proyecto e hitos, 5 solicitudes de cambio, 12 KPI de gestión del cambio y 2 tableros.

**Lo que más pesa:** la mitad de la nota está en que todo esté **conectado**: si cambio una duración, un recurso o
apruebo una solicitud, deben actualizarse Gantt, costos, KPI y tableros, y debemos poder **explicarlo**. Por eso el
sistema tiene un único servicio de recálculo y por eso los issues de pruebas E2E (A-18, B-18, C-14) son críticos.
Es preferible un sistema simple y totalmente integrado que módulos aislados muy pulidos.

## 3. Material del profesor y qué aporta

| Archivo | Qué usamos de él |
|---|---|
| `Rubrica_Proyecto_Integrador.pdf` | Indicadores, pesos y caso mínimo (sección 2) |
| `Plantilla_Seguimiento_Gestion_del_Cambio_KPI.xlsx` | Campos del registro de cambios, **las 10 etapas** (Necesidad, Evaluación, Riesgos, Planificación, Comunicación inicial, Implementación, Seguimiento, Resultados corto plazo, Correctivas, Adopción cultural), estados, **KPI-01 a KPI-10** con metas y semáforo, y el tablero ejecutivo de cambio |
| `Actividad_30_KPI.xlsx` | Cómo se define un KPI: fórmula, variables, unidad, meta u operador (>=, <=, entre límites), "No calculable" si el denominador es cero, redondeo final a 2 decimales |
| `Actividad_Gantt_Hitos.xlsx` | Caso de prueba (18 actividades, 6 etapas) y los campos de un hito (logro, actividades asociadas, semana, evidencia, indicador, criterio, validador, acción correctiva) |

Detalle de reglas en `docs/requerimientos.md`. Copien los archivos a `docs/referencias/`.

## 4. Arquitectura en 1 minuto

Monolito modular: **React + Vite + TypeScript** -> **API Spring Boot 3 (Java 21)** -> **PostgreSQL con Flyway**.
Un único despliegue. Reglas que no se rompen:

1. **Todo cambio pasa por `RecalculoService.recalcular(proyectoId)`**, que ejecuta los pasos (`PasoRecalculo`) en una transacción y en el orden de `OrdenRecalculo`: motor de fechas -> consolidación -> costos -> sobreasignación -> hitos -> KPI. Cada dev agrega **su** paso; nadie recalcula por su cuenta.
2. El **motor de dependencias** (`planificacion/motor`) es dominio puro: sin BD ni otros módulos, con tests.
3. Los **KPI se calculan** con una fórmula en texto (exp4j) sobre un catálogo de variables; el histórico guarda un snapshot por recálculo.
4. La **línea base** es una copia (`*_base`); los cambios aprobados modifican el plan vigente, no la base.
5. Los **tableros solo leen**; no recalculan.
6. Migraciones Flyway: una nueva por cambio (`V2__...sql`); **nunca editar una ya publicada en `main`**.

Más detalle en `docs/arquitectura.md` y `docs/modelo-datos.md`.

## 5. Equipo y responsabilidades

| Dev | GitHub | Módulos (backend y frontend) | Horas estimadas |
|---|---|---|---|
| **A** | @ManuelCaceresDuocUC | Modelo de datos, proyectos y EDT, motor de dependencias, recálculo, línea base, Gantt | 75 h |
| **B** | @HeOlivares | Recursos y calendario, costos, sobreasignación, hitos, KPI (proyecto y cambio), tablero de proyecto, despliegue | 78 h |
| **C** | @cristianmonsalve14 | Autenticación y roles, gestión del cambio, tablero de cambio, seed del caso de prueba, guion de demo y manual de instalación | 70 h |

Cada uno es dueño de su módulo de punta a punta (API + pantalla + pruebas) y lo explica en la demo.
El `CODEOWNERS` solicita como revisor al dueño de cada carpeta. Sin revisión obligatoria: cada uno fusiona sus propios PR cuando el CI está en verde (trabajamos en horario vespertino y sin reuniones fijas).
Las horas son estimaciones del plan: si vas con desvío, avisa en el daily para rebalancear.

## 6. Calendario (hoy 22 oct -> entrega 5 nov)

| Sprint (milestone) | Fechas | Objetivo | Horas A / B / C |
|---|---|---|---|
| Sprint 0 - Fundamentos | 2026-10-22 al 2026-10-24 | Requerimientos, modelo, contratos de API, CI, despliegue y autenticación | 10 / 13 / 10 |
| Sprint 1 - Módulos base | 2026-10-25 al 2026-10-28 | Módulos base: proyectos y EDT, motor FS/SS/FF/SF, recursos, hitos, solicitudes, seed v1 | 28 / 25 / 17 |
| Sprint 2 - Funcionalidad completa | 2026-10-29 al 2026-10-31 | Recálculo integrado, Gantt, KPI, flujo y aplicación de cambios, seed v2 | 24 / 23 / 19 |
| Sprint 3 - Integración y congelamiento | 2026-11-01 al 2026-11-02 | Tableros, pruebas E2E de los 3 recálculos, bugs y **congelamiento el 2 nov** | 8 / 14 / 15 |
| Sprint 4 - Cierre y demo | 2026-11-03 al 2026-11-05 | Documentación, guion de demo, ensayo, video de respaldo y entrega | 5 / 3 / 9 |

Qué recortar si se atrasan (en este orden): sofisticación visual del Gantt, filtros avanzados de tableros,
evidencias adjuntas (basta texto/URL). **Nunca** recortar el recálculo, las pruebas E2E ni el seed.

## 7. Quién necesita qué de quién (puntos de integración)

| Entrega | Quién la produce | Quién la consume | Cuándo debe estar |
|---|---|---|---|
| Modelo de datos V1 y contratos de API (A-01, T-02) | A (modelo), C (contratos) | B y C | 24 oct |
| Autenticación y roles (C-01) | C | B (B-04 guardas) y todos | 24 oct |
| EDT: tareas jerárquicas (A-05) | A | B (asignaciones, calendario, hitos), C (seed) | 26 oct |
| Motor de dependencias FS/SS/FF/SF (A-07, A-08) | A | A (recálculo) | 28 oct |
| `RecalculoService` (A-12) | A | B (B-16), C (C-09, tableros) | 29-30 oct |
| Pasos de costos, sobreasignación, hitos y KPI (B-16, B-14) | B | A, C (tableros) | 31 oct |
| KPI de cambio (B-17) | B (fórmulas) | C (valores de la plantilla) | 31 oct |
| Aplicar solicitud aprobada (C-09) | C | todos (tercer recálculo de la demo) | 31 oct |
| Seed completo (C-07, C-11) | C | todos (pruebas y demo) | 31 oct |

Si te bloquea algo de otro, avisa en el daily y deja un stub o un mock del contrato para seguir avanzando.

## 8. Tus issues

Cada issue vive en GitHub (`[código] título`, con descripción y criterios de aceptación), con etiqueta `dev-a`, `dev-b`
o `dev-c`, milestone del sprint y asignación. Las tareas **T-xx** son de equipo: participan los 3 y el responsable coordina.
La columna "Depende de" indica qué debe estar listo antes (puedes empezar con un mock).

### Dev A (@ManuelCaceresDuocUC): núcleo de planificación

| Código | Tarea | Sprint | Horas | Rúbrica | Depende de |
|---|---|---|---|---|---|
| T-01 | Reunión de arranque: stack, reglas de trabajo y ADR-001 | S0 | 2 | R8 | - |
| A-01 | Revisar y completar el modelo de datos (migración V1 y ER) | S0 | 4 | R1 | - |
| A-02 | Requerimientos: proyectos, EDT, planificación y Gantt | S0 | 2 | R1 | - |
| A-03 | Verificar el backend: arranque, Flyway y CI en verde | S0 | 2 | R8 | - |
| A-04 | CRUD de proyectos: crear, editar, consultar y cerrar | S1 | 3 | R2 | - |
| A-05 | EDT: etapas, actividades y subactividades con códigos automáticos | S1 | 4 | R2 | - |
| A-06 | Consolidación hacia arriba de avance, fechas y costos | S1 | 3 | R2 | A-05 |
| A-07 | Motor: dependencias FS y SS con desfases (+ tests) | S1 | 6 | R4 | - |
| A-08 | Motor: dependencias FF y SF (+ tests y ADR) | S1 | 3 | R4 | A-07 |
| A-09 | Rechazo de ciclos al crear dependencias (API y mensaje en UI) | S1 | 2 | R4 | A-05 |
| A-10 | Frontend: listado, detalle y formulario de proyectos | S1 | 3 | R2 | A-04 |
| A-11 | Frontend: árbol EDT con alta/edición y dependencias | S1 | 4 | R2 | A-05, A-09 |
| A-12 | RecalculoService: pipeline motor, consolidación y pasos de otros módulos | S2 | 6 | R4+R8 | A-06, A-07, A-08 |
| A-13 | Endpoints para cambiar duración y dependencias con recálculo | S2 | 2 | R4 | A-12 |
| A-14 | Línea base y comparación plan vs real | S2 | 3 | R4 | A-12 |
| A-15 | Gantt jerárquico: barras, avance y dependencias | S2 | 6 | R4 | A-12 |
| A-16 | Gantt: hitos, atrasos y comparación plan vs real | S2 | 3 | R4 | A-14, A-15, B-11 |
| A-17 | Gantt: editar duración y dependencias desde la UI con recálculo | S2 | 4 | R4 | A-13, A-15 |
| A-18 | Prueba E2E 1: cambiar una duración y verificar Gantt, costos, KPI y tableros | S3 | 3 | R8 | A-17, B-16, B-21 |
| A-19 | Corrección de errores de integración (Gantt, motor, EDT) | S3 | 4 | R8 | - |
| T-05 | Congelamiento de funcionalidades (2 nov) y revisión cruzada de PRs | S3 | 1 | R8 | - |
| A-20 | Documentar modelo de datos y arquitectura | S4 | 3 | R1 | - |
| T-08 | Entrega final: tag v1.0, README y revisión contra la rúbrica | S4 | 2 | R8 | - |

### Dev B (@HeOlivares): recursos, hitos, KPI, tablero de proyecto y despliegue

| Código | Tarea | Sprint | Horas | Rúbrica | Depende de |
|---|---|---|---|---|---|
| T-03 | Plan del proyecto y registro de riesgos (docs/gestion) | S0 | 3 | R8 | - |
| B-01 | Requerimientos: recursos, tiempos, costos, hitos y KPI | S0 | 2 | R1 | - |
| B-02 | Despliegue online inicial (BD + backend + frontend) | S0 | 3 | R8 | - |
| B-03 | Verificar monorepo: CI del frontend, rama protegida y plantillas | S0 | 2 | R8 | - |
| B-04 | Layout base del frontend, navegación y guardas por rol | S0 | 3 | R8 | C-01 (autenticación) |
| B-05 | CRUD de tipos de recurso y profesionales (tarifas y capacidad) | S1 | 3 | R3 | - |
| B-06 | Asignaciones recurso a tarea (cantidad y tarifa) | S1 | 3 | R3 | A-05, B-05 |
| B-07 | Calendario laboral, fechas plan/real y duración | S1 | 4 | R3 | A-05 |
| B-08 | Costo planificado y real por actividad | S1 | 4 | R3 | B-06 |
| B-09 | Alerta de sobreasignación de profesionales | S1 | 4 | R3 | B-06, B-07 |
| B-10 | Frontend: recursos, asignaciones y costos | S1 | 4 | R3 | B-05, B-06, B-09 |
| B-11 | CRUD de hitos con actividades asociadas | S1 | 3 | R4 | A-05 |
| B-12 | Evaluador de KPI: variables, fórmulas, metas y umbrales | S2 | 6 | R5 | - |
| B-13 | Catálogo de variables y los 8 KPI de proyecto e hitos | S2 | 3 | R5 | B-12, A-06, B-08 |
| B-14 | Histórico de KPI (snapshot en cada recálculo) y alertas | S2 | 3 | R5 | B-12 |
| B-15 | Administrador de KPI en el frontend | S2 | 4 | R5 | B-12, B-14 |
| B-16 | Integrar costos, sobreasignación e hitos como pasos del recálculo | S2 | 3 | R3+R8 | A-12, B-08, B-09, B-11 |
| B-17 | KPI de gestión del cambio: los 10 de la plantilla y 3 adicionales | S2 | 4 | R5 | B-12, C-03, C-04 |
| B-21 | Tablero de comando del proyecto | S3 | 6 | R7 | A-12, B-14, B-16 |
| B-18 | Prueba E2E 2: modificar recursos y verificar costos, sobreasignación y KPI | S3 | 3 | R8 | B-16, B-21 |
| B-19 | Corrección de errores de integración (recursos, costos, KPI, hitos) | S3 | 4 | R8 | - |
| T-06 | Reporte de avance semana 2 y actualización de riesgos | S3 | 1 | R8 | - |
| B-20 | Manual de usuario: proyectos, EDT, recursos, Gantt, hitos y KPI | S4 | 3 | R8 | - |

### Dev C (@cristianmonsalve14): cambio, tablero de cambio, auth, seed y docs

| Código | Tarea | Sprint | Horas | Rúbrica | Depende de |
|---|---|---|---|---|---|
| T-02 | Acordar contratos de API entre módulos (docs/api) | S0 | 3 | R1 | A-01 (modelo de datos) |
| C-01 | Autenticación JWT y 2 roles (ADMIN / USUARIO_PROYECTO) | S0 | 5 | R8 | - |
| C-02 | Requerimientos: gestión del cambio, tableros e integración | S0 | 2 | R1 | - |
| C-03 | Solicitudes de cambio: modelo, CRUD y filtros | S1 | 4 | R6 | - |
| C-04 | Las 10 etapas del cambio con control de estado | S1 | 3 | R6 | C-03 |
| C-05 | Evidencias e historial de trazabilidad por solicitud | S1 | 3 | R6 | C-03 |
| C-06 | Frontend: listado, filtros y formulario de solicitudes | S1 | 4 | R6 | C-03, C-04 |
| C-07 | Seed v1: proyecto, EDT, profesionales y recursos | S1 | 3 | R8 | A-01, A-05 |
| C-08 | Flujo de cambio: transiciones válidas y aprobación o rechazo | S2 | 4 | R6 | C-03, C-04, C-05 |
| C-09 | Aplicar solicitud aprobada: modifica el plan y dispara el recálculo | S2 | 6 | R6+R8 | C-08, A-12, B-16 |
| C-10 | Frontend: detalle de solicitud, etapas, evidencias y aprobación | S2 | 4 | R6 | C-06, C-08 |
| C-11 | Seed v2: dependencias, asignaciones, hitos, solicitudes y KPI | S2 | 4 | R8 | C-07, A-12, B-11, B-17 |
| T-04 | Reporte de avance semana 1 | S2 | 1 | R8 | - |
| C-12 | Tablero de comando de gestión del cambio | S3 | 6 | R7 | C-04, B-17 |
| C-13 | Filtros y actualización dinámica de ambos tableros | S3 | 2 | R7 | B-21, C-12 |
| C-14 | Prueba E2E 3: aprobar una solicitud y verificar plan, costos, KPI y tableros | S3 | 3 | R8 | C-09, B-21, C-12 |
| C-15 | Corrección de errores de integración (cambio, tableros, auth, seed) | S3 | 4 | R8 | - |
| C-16 | Guion de demo y casos de prueba documentados | S4 | 4 | R8 | - |
| C-17 | Manual de instalación y despliegue | S4 | 2 | R8 | - |
| T-07 | Ensayo general de la demo y video de respaldo | S4 | 3 | R8 | - |

Total de issues: **66** (el tablero del proyecto los trae todos con estimación y fechas del sprint).

## 9. Cómo trabajamos

**Git (trunk-based con ramas cortas)**
- Todo entra a `main` por PR y cada uno fusiona el suyo (squash) cuando el CI está en verde y ya probó el cambio; no se requiere aprobación de otro. Nunca se sube directo a `main`.
- Rama por issue: `feat/<n°>-<descripcion>`, `fix/<n°>-<descripcion>`. Commits `feat:`, `fix:`, `docs:`, `test:`.
- PR pequeños y frecuentes (idealmente menos de un día). El PR dice `Closes #<n°>` y completa el checklist de la plantilla.
- Los archivos compartidos (`config/`, `common/`, `App.tsx`, `api/`) se tocan con PR chicos para evitar conflictos.

**Definition of Done:** código en `main`, con pruebas, desplegado, documentado y con el issue cerrado.

**Seguimiento remoto**
- **Daily asíncrono** (chat del equipo, 3 líneas): qué hice, qué haré, qué me bloquea.
- **Sin reuniones fijas:** las decisiones se escriben en el chat o en el issue y, si nadie objeta en 24 h, quedan aprobadas. Una llamada corta nocturna solo para ensayar la demo (T-07).
- Actas, riesgos, decisiones (ADR), cambios del propio proyecto y reportes de avance en `docs/gestion/`.
- Cada bug es un issue (`tipo-bug`) enlazado a la bolsa de bugs de su dueño; cada cambio de alcance es un issue "Cambio del propio proyecto".

**GitHub Project (tablero del desarrollo).** Viene creado, vinculado al repo y con todos los issues, con los campos
`Estimación (h)`, `Inicio` y `Fin` cargados. La API de GitHub no permite crear vistas, así que se hacen en 2 minutos desde la web:
1. **Tablero:** vista Board agrupada por *Status*; agrega la opción "En revisión" al campo Status.
2. **Por persona:** vista Table agrupada por *Assignees*, mostrando Milestone, Estimación y Labels.
3. **Roadmap:** vista Roadmap usando `Inicio` y `Fin`, agrupada por Milestone (es el "Gantt" del propio desarrollo).
4. **Insights:** gráfico de avance (burndown) por Milestone; capturarlo en los reportes semanales.
5. En *Workflows* del proyecto, activa que los issues cerrados pasen a Done y que los nuevos entren a Todo.

**Etiquetas:** `dev-a/b/c`, `equipo`, `m-<módulo>`, `tipo-feature|bug|docs|test`, `p1|p2|p3`, `rubrica-1..8`.

## 10. Caso de prueba y demostración

El seed (`db/seed/caso-prueba.json`) deja el proyecto "Habilitar un espacio de estudio colaborativo" con **4 etapas,
12 actividades, 20 subactividades, 35 dependencias (FS, SS, FF y SF, con desfases), 5 profesionales, 4 tipos de recurso,
6 hitos y 5 solicitudes de cambio**, más 8 KPI de proyecto e hitos y 13 de cambio (`kpi-definiciones.json`).
Crece con cada sprint (C-07, C-11) y se reinicia con `scripts/reset-demo.sh`.

La demostración (guion en `docs/pruebas/guion-demo.md`) muestra, comprobando efectos en **Gantt, costos, KPI y tableros**:
1. **Cambiar una duración** (A-18).
2. **Modificar recursos** (B-18).
3. **Aprobar una solicitud** (C-14).
Además: el rol USUARIO_PROYECTO y la explicación de por qué cambió cada número.

## 11. Pendiente de aclarar con el profesor

1. **KPI de cambio:** la rúbrica pide 12 e indica "tres adicionales", y la plantilla trae 10 (KPI-01..10). Implementamos 13
   (10 + 3 propuestos) para cumplir en cualquier lectura; confirmar cuál es la esperada y qué KPI adicionales prefiere.
2. **EDT:** se modela como una sola tabla jerárquica (sin módulo propio); confirmar que basta con la jerarquía de 3 niveles y los códigos.
3. **Semántica de SF y desfases negativos:** documentarla en un ADR (A-08) y mostrarla en la demo.
4. **Caso de prueba:** confirmar si podemos usar el caso del espacio de estudio o si deben ser datos propios.
5. **Duración de la demo y formato de entrega** (repositorio, documentación, video).

## 12. Primeros pasos (hoy)

- [ ] Aceptar la invitación al repositorio y al proyecto de GitHub.
- [ ] Clonar, copiar `.env.example` a `.env` y correr `docker compose up --build`; verificar `http://localhost:8080/api/health`.
- [ ] Acordar por escrito en el chat el stack definitivo (ADR-001, T-01).
- [ ] Cada uno abre su primer issue del Sprint 0 y crea su rama.
- [ ] Copiar los archivos del profesor a `docs/referencias/`.
