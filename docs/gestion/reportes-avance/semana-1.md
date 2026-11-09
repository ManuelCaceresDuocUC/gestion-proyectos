# Reporte de avance - Semana 1 (T-04)

- **Período oficial (guía):** Sprint 0 y 1, 22–28 oct 2026.
- **Fecha de este informe:** 9 nov 2026. El issue #98 era de Sprint 2; se entrega tarde.
- **Corte de cifras:** GitHub al 9 nov 2026 (41 closed + 26 open). La guía lista 66; el tablero tiene **67** por A-21.
- **Avance:** **41 / 67** issues cerrados (**61 %**). Horas guía cerradas ≈ **139 / 223 h** (**62 %**); A-21 no tiene horas en la tabla.
- **Hitos alcanzados:** modelo V1+V21, motor y recálculo, Gantt, recursos/costos/sobreasignación, solicitudes (CRUD, etapas, flujo, aplicar), seed v1/v2, JWT, requerimientos C-02, contratos T-02, front de recursos (B-10), despliegue Render (B-02).
- **Desvíos y causas:** la entrega prevista era el 5 nov; el congelamiento del 2 nov no se hizo (T-05 abierto). Avanza A; B y C se atrasaron en S0 (auth, contratos, layout, hitos, KPI). C-05 se dejó para el final. El seed C-11 no carga hitos ni KPI (no hay servicio).
- **Riesgos nuevos y acciones:** no hay ID nuevo. R-01 mitigado (A-07/A-08). R-02 mitigado en parte (T-02 en main). R-04 mitigado en parte (seed v1/v2). Siguen R-03 (E2E), R-05 (KPI 12 vs 13), R-06 (demo) y R-08 (docs).
- **Plan de la próxima semana:** B: hitos y KPI (B-11, B-12…B-17) y tablero de proyecto (B-21). C: C-12 cuando exista B-17; evidencias C-05 al final. Equipo: T-05/T-06, E2E (B-18, C-14) y no recortar recálculo ni seed.

Fuente de horas: `docs/GUIA_DESARROLLO.md`. Un issue cerrado ≠ demo lista (faltan KPI, hitos, tableros).

## Avance por sprint (issues)

| Sprint | Fechas guía | Cerrado (ejemplos) | Sigue abierto |
|---|---|---|---|
| 0 Fundamentos | 22–24 oct | T-01, A-01…A-03, B-01…B-03, C-01, C-02, T-02 | T-03, B-04 (layout) |
| 1 Módulos base | 25–28 oct | A-04…A-11, B-05…B-10, C-03, C-04, C-06, C-07 | B-11 (hitos), C-05 (evidencias) |
| 2 Funcionalidad | 29–31 oct | A-12…A-17, A-21, C-08…C-11 | B-12…B-17, T-04 (este reporte) |
| 3 Integración | 1–2 nov | A-18 (cerrado 31 oct) | Tableros, E2E 2 y 3, bolsas de bugs, T-05 |
| 4 Cierre | 3–5 nov | A-20 (cerrado 31 oct) | Manuales, guion, ensayo, T-08 |

## Horas estimadas vs cerradas (guía)

| Dev | Issues cerrados | Horas cerradas / estimadas | % h |
|---|---|---|---|
| A (Manuel) | 20 + A-21 | 68 / 75 | 91 % |
| B (Héctor) | 9 | 29 / 78 | 37 % |
| C (Cristian) | 11 | 42 / 70 | 60 % |
| **Equipo** | **41 / 67** | **139 / 223** | **62 %** |

Cerradas de A (etiqueta `dev-a`): T-01, A-01…A-18, A-20. A-21 (#138) no lleva `dev-a`.

Cerradas de B: B-01, B-02, B-03, B-05, B-06, B-07, B-08, B-09, B-10. T-03 sigue abierto.

Cerradas de C: T-02, C-01, C-02, C-03, C-04, C-06, C-07, C-08, C-09, C-10, C-11.

## Decisiones (ADR / cambios del propio proyecto)

| ID | Qué |
|---|---|
| ADR-001 | Stack Spring + React + PostgreSQL |
| ADR-002 / ADR-003 | Motor de fechas y un solo `RecalculoService` |
| ADR-004 / CP-01 (29 oct) | Guía del profesor: V21, roles ADMIN / USUARIO_PROYECTO, estados de solicitud |
| ADR-005 | Gantt SVG propio |
| ADR-006 | Protección de `main` (B-03) |
| ADR-007 | Carga diaria = horas / días hábiles (B-09) |

## Capturas (GitHub Project)

Pendientes en esta carpeta (criterio de T-04; no estaban al cortar las cifras):

- `semana-1-tablero.png` — vista Board del Project
- `semana-1-burndown.png` — Insights / burndown

Cómo sacarlas: repo → Projects → tablero; Insights → gráfico de avance por Milestone.
