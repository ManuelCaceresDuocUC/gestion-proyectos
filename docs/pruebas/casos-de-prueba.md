# Casos de prueba

Corte: 9 nov 2026. Guion: `docs/pruebas/guion-demo.md`. Seed: `db/seed/README.md` y `SeedServiceTest`.

Maven local ese día no corrió (`Flyway` no conectó a Postgres). Lo de abajo es el código de test en `main` y lo que el seed **carga de verdad**, no una corrida de demo con capturas.

## Verificación del caso mínimo (rúbrica)

El JSON trae el caso completo. El cargador (C-07 + C-11 + B-11) crea los hitos. Los KPI **no** los inserta el seed: los crea `KpiService` en el recálculo (C-12).

| Elemento | Requerido | En el JSON | Cargado | OK |
|---|---|---|---|---|
| Etapas | 4 | 4 | 4 (`TipoTarea.ETAPA`) | Sí |
| Actividades | 12 | 12 | 12 | Sí |
| Subactividades | 20 | 20 | 20 | Sí |
| Tipos de dependencia | 4 (FS, SS, FF, SF) | los 4 (35 filas; hay desfase negativo) | 35, los 4 tipos | Sí |
| Profesionales | 5 | P1…P5 | 5 | Sí |
| Tipos de recurso | 4 | Profesional, Equipamiento, Material, Servicio externo | 4 | Sí |
| Hitos | 6 | H1…H6 | 6, `PENDIENTE`, sin fecha real (B-11) | Sí |
| KPI de proyecto e hitos | 8 | KP-01…KP-08 | 0 en seed; 8 tras recálculo | Parcial |
| Solicitudes de cambio | 5 | SC-001… en JSON → `SC-01`…`SC-05` | 5, `ABIERTO`, 10 etapas cada una | Sí |
| KPI de gestión del cambio | 12 | 13 (KC-01…KC-13) | 0 en seed; 13 tras recálculo (C-12) | Parcial |
| Tableros | 2 | — | 2 (`/tableros` proyecto B-21 y cambio C-12; filtros C-13) | Sí |

También cargado, fuera de esa tabla: 7 recursos no profesionales, 39 asignaciones, línea base v1, alerta P2 en 1.1.2, usuarios `admin@demo.cl` / `usuario@demo.cl`.

## Resumen de los 3 recálculos

| ID | Acción | Resultado esperado (Gantt, costos, KPI, tableros) | Obtenido | Responsable |
|---|---|---|---|---|
| CP-01 | Cambiar duración de una hoja (A-18) | Sucesoras FS/FF se corren; SS/SF no; consolidación; línea base igual; costos si hay asignaciones; KPI y tablero de proyecto | Gantt y línea base: **sí** (E2E). Tableros: **sí** API (B-21, C-12, C-13). Costos/KPI en A-18: **no asertados** | A |
| CP-02 | Modificar cantidad o recurso (B-18) | Costo plan/real; alerta de sobreasignación; KPI; tablero | E2E HTTP y UI del seed, 10 nov 2026. Hoja 1.440.000→1.280.000; proyecto 20.488.000→20.328.000; alerta se apaga; KP-08 = 0; KP-03 no calculable. Capturas en `docs/pruebas/capturas/` | B |
| CP-03 | Aprobar y aplicar una solicitud **TIEMPO** (C-14). Las 5 del seed no son TIEMPO | Aprobar no mueve. Aplicar suma días, `EN_IMPLEMENTACION`, `aplicada=true`; Gantt; línea base igual; KPI de cambio; tablero de cambio | **sí** E2E HTTP (`RecalculoSolicitudE2ETest`, 10 nov 2026). Seed: 0 TIEMPO. Capturas: T-07 | C |

Capturas de CP-02: `docs/pruebas/capturas/cp-02-*.png`. CP-01 y CP-03: pendientes (T-07, fecha y archivos por confirmar). Plan: `docs/pruebas/T-07-ensayo-demo.md`.

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
| Costos / KPI / tableros | — | Sin aserción en A-18. Tableros: B-21, C-12, C-13 |

---

## CP-02 — Modificar recursos

**Issue:** B-18 (abierto). Backend: B-06, B-08, B-09, B-10. `AsignacionService` llama `RecalculoService`.

### Datos de entrada (demo / seed)

Hoja **1.1.2**, P2 (Ingeniero de infraestructura, tarifa 32.000, capacidad 8) cantidad **45** h, duración 5 d → 9 h/día. Bajar a **40**.

### Esperado

Costo plan = cantidad × tarifa: 1.440.000 → 1.280.000 CLP. Alerta si carga **>** capacidad (`PasoSobreasignacion`); a 8 h/día desaparece. Costo real sigue 0 si no hay `cantidadReal`. Fechas del Gantt no tienen por qué moverse. KPI: snapshot del recálculo. Tablero de proyecto: B-21.

### Obtenido

| Prueba | Fecha | Resultado |
|---|---|---|
| `PasoCostosTest`, `PasoSobreasignacionTest`, API de asignaciones | en `main` (B-08/B-09) | Costos y alertas en backend |
| Seed: P2 en 1.1.2 dispara alerta | `SeedServiceTest` (si hay asignaciones) | Nombre «Ingeniero de infraestructura» |
| `RecalculoRecursosE2ETest` | 10 nov 2026 | 1 prueba, 0 fallos. Ver comando abajo |
| UI seed `EEC-2026`, P2 en 1.1.2, 45→40 h | 10 nov 2026 | Costo de la hoja $1.440.000→$1.280.000; tablero $20.488.000→$20.328.000, 1 de 5→0 de 5; KP-08 = 0 VERDE; KP-03 no calculable. Fechas 07-09-2026→11-09-2026. Capturas abajo. Seed devuelto a 45 h |

Comando, desde `backend/`:

```text
mvn -q test -Dtest=RecalculoRecursosE2ETest
```

Surefire: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 20.28 s`.

El test crea un proyecto aislado (hoja de 5 días, profesional tarifa 32.000 y capacidad 8), hace `POST /api/asignaciones` con 45 h y luego `PUT` a 40 h. Compara el snapshot anterior con el posterior, no solo contra constantes:

- Costo plan de la asignación, de la hoja y del proyecto: 1.440.000,00 → 1.280.000,00. El tablero (`costos.plan`) coincide con el proyecto en cada momento. Costo real 0 en asignación, hoja, proyecto y tablero, igual antes y después.
- Alertas: lista no vacía → vacía. `recursos.sobreasignados` del tablero: mayor que 0 → 0.
- KP-08 calculable en ambos lados; el valor de después es menor que el de antes y compara igual a 0 (la escala del JSON no se fija en el test). El histórico de KP-08 suma una fila: la última es el valor nuevo y la anterior es el valor previo. El KPI del tablero coincide con `GET /api/proyectos/{id}/kpi`.
- KP-03 (`VALOR_GANADO / COSTO_REAL`) queda `noCalculable: true` en los dos estados. El CPI no cambia porque `cantidadReal` sigue en 0: la API de asignaciones no recibe cantidad real y el costo real no sale de cero, así que el denominador es 0.
- `inicioPlan` y `finPlan` de la hoja, y `fechaFinPlan` del proyecto, iguales antes y después del `PUT`.

Capturas del 10 nov 2026, con la imagen de Docker reconstruida (`GET /api/tablero/cambio/1344` = 200). Seed `EEC-2026`, hoja 1.1.2, P2 de 45 h a 40 h por la pantalla de la EDT, y luego la cantidad se devolvió a 45 para dejar el caso como al inicio:

- `docs/pruebas/capturas/cp-02-recursos-antes.png`: alerta, 9 h frente a 8 h, tarea 1.1.2.
- `docs/pruebas/capturas/cp-02-recursos-despues.png`: «Ningún profesional supera su capacidad diaria.»
- `docs/pruebas/capturas/cp-02-tablero-antes.png`: 1 de 5, plan $20.488.000, filas de 9 h.
- `docs/pruebas/capturas/cp-02-tablero-despues.png`: 0 de 5, plan $20.328.000, sin filas. En la misma página, KP-03 «No calculable» y KP-08 = 0 VERDE.

---

## CP-03 — Aprobar y aplicar TIEMPO

**Issue:** C-14 (#110). Detalle: `docs/pruebas/C-14-recalculo-solicitud.md`. Código: C-08, C-09, C-10. E2E: `integracion/RecalculoSolicitudE2ETest`. Servicio: `SolicitudCambioServiceTest` (`aplicarTiempoSumaImpactoALaHoja`, `aplicarTiempoMueveElPlanYConservaLaLineaBase`, `aplicarCostoNoMueveLaDuracion`).

### Datos de entrada

Seed: 5 solicitudes, tipos `TECNOLOGIA` u `ORGANIZACION`, `ABIERTO`, no aplicadas. **Ninguna es TIEMPO.**

Demo (guion): editar `SC-01` a tipo Tiempo (hoja **3.1.1**, 5 d, impacto +5 → 10 d; FS a 3.1.2 y 3.2.1) o alta nueva TIEMPO. Flujo: Enviar a evaluación → Aprobar → Aplicar al plan.

Test C-09: hoja 5 d, impacto +3, línea base fijada.

### Esperado

Aprobar: estado `APROBADO`, plan igual, sin snapshot KPI nuevo. Aplicar TIEMPO: duración 5+3=8, `aplicada=true`, `EN_IMPLEMENTACION`, `finPlan` después, `finBase` igual, costo plan igual, KPI y ambos tableros leen el recálculo. Aplicar no-TIEMPO: no llama a `CronogramaService`; solo marca aplicada. C-13 refresca `/tableros` al volver a la pestaña.

### Obtenido

| Prueba | Fecha | Resultado |
|---|---|---|
| `aplicarTiempoSumaImpactoALaHoja` | merge C-09 (5 nov, PR #154) | duración 8, aplicada, `EN_IMPLEMENTACION` |
| `aplicarTiempoMueveElPlanYConservaLaLineaBase` | idem | `finPlan` avanza; bases igual |
| `aplicarCostoNoMueveLaDuracion` | idem | duración 5 |
| Seed 5 solicitudes no TIEMPO | C-11 | `SeedServiceTest` |
| `RecalculoSolicitudE2ETest` | 10 nov 2026 | Aprobar no mueve; aplicar TIEMPO mueve A/B, línea base y costo plan iguales, KPI + tableros |

ADMIN aprueba; `USUARIO_PROYECTO` no (C-01).
