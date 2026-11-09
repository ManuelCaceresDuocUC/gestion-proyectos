# Casos de prueba

Corte: 9 nov 2026. Guion: `docs/pruebas/guion-demo.md`. Seed: `db/seed/README.md` y `SeedServiceTest`.

Maven local ese día no corrió (`Flyway` no conectó a Postgres). Lo de abajo es el código de test en `main` y lo que el seed **carga de verdad**, no una corrida de demo con capturas.

## Verificación del caso mínimo (rúbrica)

El JSON trae el caso completo. El cargador (C-07 + C-11 + B-11) crea los hitos. Los KPI siguen sin servicio.

| Elemento | Requerido | En el JSON | Cargado | OK |
|---|---|---|---|---|
| Etapas | 4 | 4 | 4 (`TipoTarea.ETAPA`) | Sí |
| Actividades | 12 | 12 | 12 | Sí |
| Subactividades | 20 | 20 | 20 | Sí |
| Tipos de dependencia | 4 (FS, SS, FF, SF) | los 4 (35 filas; hay desfase negativo) | 35, los 4 tipos | Sí |
| Profesionales | 5 | P1…P5 | 5 | Sí |
| Tipos de recurso | 4 | Profesional, Equipamiento, Material, Servicio externo | 4 | Sí |
| Hitos | 6 | H1…H6 | 6, `PENDIENTE`, sin fecha real (B-11) | Sí |
| KPI de proyecto e hitos | 8 | KP-01…KP-08 | 0 (B-12/B-13) | No |
| Solicitudes de cambio | 5 | SC-001… en JSON → `SC-01`…`SC-05` | 5, `ABIERTO`, 10 etapas cada una | Sí |
| KPI de gestión del cambio | 12 | 13 (KC-01…KC-13) | 0 (B-17) | No |
| Tableros | 2 | — | 0 (`/tableros` texto pendiente B-21/C-12) | No |

También cargado, fuera de esa tabla: 7 recursos no profesionales, 39 asignaciones, línea base v1, alerta P2 en 1.1.2, usuarios `admin@demo.cl` / `usuario@demo.cl`.

## Resumen de los 3 recálculos

| ID | Acción | Resultado esperado (Gantt, costos, KPI, tableros) | Obtenido | Responsable |
|---|---|---|---|---|
| CP-01 | Cambiar duración de una hoja (A-18) | Sucesoras FS/FF se corren; SS/SF no; consolidación; línea base igual; costos si hay asignaciones; KPI y tablero de proyecto | Gantt y línea base: **sí** (E2E). Costos/KPI/tableros: **no medidos** (no hay KPI ni tablero) | A |
| CP-02 | Modificar cantidad o recurso (B-18) | Costo plan/real; alerta de sobreasignación; KPI; tablero | Costos y alerta: **sí en backend** (B-08/B-09). E2E B-18 y KPI/tablero: **pendiente** | B |
| CP-03 | Aprobar y aplicar una solicitud **TIEMPO** (C-14). Las 5 del seed no son TIEMPO | Aprobar no mueve. Aplicar suma días, `EN_IMPLEMENTACION`, `aplicada=true`; Gantt; línea base igual; KPI de cambio; tablero de cambio | Aplicar TIEMPO y línea base: **sí en servicio** (C-09). E2E C-14, KPI y tablero: **pendiente**. Seed: 0 TIEMPO | C |

Capturas: no hay (`docs/pruebas/capturas/`). Van con el ensayo T-07.

---

## CP-01 — Cambiar una duración

**Issue:** A-18. **Detalle:** `docs/pruebas/A-18-recalculo-duracion.md`. **Test:** `RecalculoDuracionE2ETest` (merge 31 oct 2026, PR #144).

### Datos de entrada (escenario del test, no el seed)

Proyecto inicio lunes 2-nov-2026. Hoja A 5 d; B FS; C SS +1; D FF; E SF. Línea base v1. `PATCH /api/tareas/{A}` `{"duracionDias":7}`.

En la demo (guion): hoja **1.1.1** del seed (10 d → 12). Sucesora FS **1.2.1**. No es el grafo de cuatro tipos del test.

### Esperado

| Tarea | Desviación inicio | Desviación fin |
|---|---|---|
| A (1.1) | 0 | +2 |
| B (1.2) FS | +2 | +2 |
| C (1.3) SS | 0 | 0 |
| D (1.4) FF | — | +2 |
| E (1.5) SF | — | 0 |
| Proyecto | — | +2 |

Línea base igual. Volver a 5 d deja desviación 0. Ciclo B→A = 409 `CICLO_EN_DEPENDENCIAS`.

### Obtenido

| Prueba | Fecha | Resultado |
|---|---|---|
| `RecalculoDuracionE2ETest` (HTTP, como el Gantt) | 31 oct 2026 (CI del PR) | A/B/D se mueven; C/E no; `finBase` igual; ciclo 409 |
| Manual Gantt sobre `EEC-2026` | — | No ejecutada en C-16 |
| Costos / KPI / tableros | — | No hay aserción. KPI y tableros no existen |

---

## CP-02 — Modificar recursos

**Issue:** B-18 (abierto). Backend: B-06, B-08, B-09, B-10. `AsignacionService` llama `RecalculoService`.

### Datos de entrada (demo / seed)

Hoja **1.1.2**, P2 (Ingeniero de infraestructura, tarifa 32.000, capacidad 8) cantidad **45** h, duración 5 d → 9 h/día. Bajar a **40**.

### Esperado

Costo plan = cantidad × tarifa: 1.440.000 → 1.280.000 CLP. Alerta si carga **>** capacidad (`PasoSobreasignacion`); a 8 h/día desaparece. Costo real sigue 0 si no hay `cantidadReal`. Fechas del Gantt no tienen por qué moverse. KPI y tablero: cuando existan.

### Obtenido

| Prueba | Fecha | Resultado |
|---|---|---|
| `PasoCostosTest`, `PasoSobreasignacionTest`, API de asignaciones | en `main` (B-08/B-09) | Costos y alertas en backend |
| Seed: P2 en 1.1.2 dispara alerta | `SeedServiceTest` (si hay asignaciones) | Nombre «Ingeniero de infraestructura» |
| E2E B-18, KPI, tableros, corrida 45→40 en UI | — | No |

---

## CP-03 — Aprobar y aplicar TIEMPO

**Issue:** C-14 (abierto). Código: C-08, C-09, C-10. Tests: `SolicitudCambioServiceTest` (`aplicarTiempoSumaImpactoALaHoja`, `aplicarTiempoMueveElPlanYConservaLaLineaBase`, `aplicarCostoNoMueveLaDuracion`).

### Datos de entrada

Seed: 5 solicitudes, tipos `TECNOLOGIA` u `ORGANIZACION`, `ABIERTO`, no aplicadas. **Ninguna es TIEMPO.**

Demo (guion): editar `SC-01` a tipo Tiempo (hoja **3.1.1**, 5 d, impacto +5 → 10 d; FS a 3.1.2 y 3.2.1) o alta nueva TIEMPO. Flujo: Enviar a evaluación → Aprobar → Aplicar al plan.

Test C-09: hoja 5 d, impacto +3, línea base fijada.

### Esperado

Aprobar: estado `APROBADO`, plan igual. Aplicar TIEMPO (test): duración 5+3=8, `aplicada=true`, `EN_IMPLEMENTACION`, `finPlan` después, `finBase` igual. Aplicar no-TIEMPO: no llama a `CronogramaService`; solo marca aplicada. KPI de cambio y tablero: cuando existan.

### Obtenido

| Prueba | Fecha | Resultado |
|---|---|---|
| `aplicarTiempoSumaImpactoALaHoja` | merge C-09 (5 nov, PR #154) | duración 8, aplicada, `EN_IMPLEMENTACION` |
| `aplicarTiempoMueveElPlanYConservaLaLineaBase` | idem | `finPlan` avanza; bases igual |
| `aplicarCostoNoMueveLaDuracion` | idem | duración 5 |
| Seed 5 solicitudes no TIEMPO | C-11 | `SeedServiceTest` |
| E2E C-14 sobre Gantt+KPI+tableros | — | No. KPI y tableros no existen |

ADMIN aprueba; `USUARIO_PROYECTO` no (C-01).
