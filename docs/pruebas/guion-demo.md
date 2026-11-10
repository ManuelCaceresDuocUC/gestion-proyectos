# Guion de demo

Duración objetivo: la que indique el profesor. Esta versión cabe en **~15 min**; si pide menos, se recorta la presentación (min 1–5) y se dejan los 3 recálculos. Ensayar con cronómetro (T-07).

- **Caso:** proyecto seed `EEC-2026` «Habilitar un espacio de estudio colaborativo».
- **Quién habla:** A = planificación y Gantt · B = recursos, costos y sobreasignación · C = cambio, auth y cierre.
- **Entrar como** `admin@demo.cl` (`ADMIN`). Contraseña: `db/seed/caso-prueba.json` (no se dicta en voz alta).
- **Tras cada recálculo** se mira Gantt y costos. Hitos: H1–H6 en Pendiente (`/hitos`). Tablero de cambio: `/tableros` (C-12). Administrador de KPI y tablero de proyecto: pendiente (B-15, B-21). No se inventan números.

Especificación de los 3 recálculos: `docs/requerimientos.md` (C-02). Resultados: `docs/pruebas/casos-de-prueba.md` y A-18.

## Antes de empezar (fuera de tiempo)

PowerShell, carpeta del repo:

```powershell
docker compose down -v
$env:APP_SEED_ENABLED = "true"
docker compose up -d --build
```

Esperar a que el API responda (`/api/health`). Front: el del compose o Vite. Si `EEC-2026` ya estaba a medias, el reset borra volúmenes y deja el seed limpio.

## Minuto a minuto

| Min | Quién | Qué muestra | Qué dice |
|---|---|---|---|
| 0:00 | C | Login → Ingresar | Roles `ADMIN` y `USUARIO_PROYECTO`. JWT. Aprobar/Rechazar solo ADMIN. |
| 0:45 | A | Proyectos → `EEC-2026` | Caso mínimo: 4 etapas, 12 actividades, 20 subactividades. EDT de 3 niveles; 1.4 es hoja. |
| 2:00 | A | Gantt (`/gantt?id=…`) | Barras, dependencias FS/SS/FF/SF, línea base v1 (el seed la fija). Clic en una hoja: Plan vs Línea base. |
| 3:30 | B | Recursos y costos | 4 tipos, 5 profesionales (P1…P5). En el proyecto: alerta de P2 en **1.1.2** (45 h / 5 días = 9 h/día > capacidad 8). Costo plan = cantidad × tarifa. |
| 5:00 | C | Cambios (elegir `EEC-2026`) | 5 solicitudes `SC-01`…`SC-05`, estado `ABIERTO`, 10 etapas en Pendiente. Tipos del seed: Tecnología u Organización; **ninguna es Tiempo**. En el detalle: evidencias (texto/URL, sin archivos) e historial. Hitos: H1–H6 en Pendiente. `/tableros`: conteos de cambio (C-12); el de proyecto dice pendiente B-21. Los KC salen tras un recálculo. |
| 6:00 | A | **Recálculo 1** | Ver bloque abajo. |
| 9:00 | B | **Recálculo 2** | Ver bloque abajo. |
| 11:30 | C | **Recálculo 3** | Ver bloque abajo. |
| 14:00 | C | Salir → `usuario@demo.cl` | Puede editar y aplicar; **no** ve Aprobar ni Rechazar. |

## Recálculo 1 — cambiar una duración (A)

**Issue:** A-18. Detalle numérico del ejemplo chico: `docs/pruebas/A-18-recalculo-duracion.md`. En la demo se usa el seed.

1. Gantt de `EEC-2026`. Clic en la hoja **1.1.1** (Encuestar a usuarios). Dura **10** d hábiles (2 semanas). Anotar Plan y Línea base.
2. Duración → **12** (+2) → **Aplicar y recalcular**.
3. Sucesora en el seed: **FS a 1.2.1** (Priorizar requisitos). 1.2.1 ya espera a 1.1.1 (10 d) y no a 1.1.2 (5 d), así que 1.2.1 y la cadena FS se corren. Esta hoja **no** tiene SS/FF/SF: esos cuatro tipos se ven en A-18, no aquí.
4. Línea base **igual**. El panel lista las tareas que movieron fechas; 1.1 y el proyecto consolidan.
5. Costos: P1 24 h y P4 8 h × tarifa; **cantidad igual ⇒ costo plan igual**. Carga diaria baja (horas / 12 d).
6. `/tableros`: el de cambio ya tiene conteos; los KC aparecen si el recálculo guardó snapshot. Tablero de proyecto: pendiente B-21. `/kpi` sin pantalla de administración (B-15).

No usar 1.1.2 aquí: esa hoja es del recálculo 2.

## Recálculo 2 — modificar recursos (B)

**Issue:** B-18 (E2E aún abierto). El recálculo de asignaciones **sí está** (B-06/B-08/B-09/B-10).

1. Gantt o Proyectos → EDT → recursos de la hoja **1.1.2**. P2 tiene cantidad **45**.
2. Mostrar `/recursos` → proyecto `EEC-2026`: alerta de sobreasignación.
3. Bajar cantidad de P2 a **40** (40 / 5 d = 8 = capacidad; alerta si carga **>** capacidad, así que **desaparece**). Guardar: dispara `RecalculoService`.
4. Costo plan de 1.1.2: 45 × 32.000 = 1.440.000 → 40 × 32.000 = **1.280.000** CLP. Fechas del Gantt no tienen por qué moverse.
5. KPI / tablero de proyecto: pendiente B-15 / B-21.

Si se sube la cantidad en vez de bajarla, la alerta se mantiene o empeora; decir el mismo criterio (carga diaria > capacidad).

## Recálculo 3 — aprobar una solicitud (C)

**Issue:** C-14 (E2E abierto). Aprobar y aplicar **sí están** (C-08, C-09, C-10).

Las 5 del seed **no son TIEMPO**: aplicarlas no mueve el Gantt (aviso en el detalle). Para el recálculo de la rúbrica hace falta una de tipo Tiempo.

**Camino corto (recomendado):** abrir `SC-01` (Ampliar capacidad de 20 a 30 puestos) → Editar → tipo **Tiempo** → guardar. Ya trae la hoja **3.1.1** (5 d hábiles) e impacto **+5** d.

**Camino largo:** Cambios → Nueva solicitud → nombre + tipo Tiempo + Tarea (id numérico de una hoja) + Impacto (días) = 2. El código lo pone el servidor (`SC-06`…).

Luego, en el detalle:

1. Anotar en el Gantt Plan y Línea base de esa hoja.
2. **Enviar a evaluación** → estado `EN_EVALUACION`. El plan no cambia.
3. **Aprobar** (solo ADMIN) → `APROBADO`. El plan **sigue igual**.
4. **Aplicar al plan** (confirma: la línea base no cambia) → `EN_IMPLEMENTACION`, `aplicada=true`. TIEMPO: 3.1.1 pasa de 5 a **10** d; FS a **3.1.2** y **3.2.1**. Costo plan igual (cantidades P2 32 h y P5 8 h). Línea base igual.
5. `/tableros` (C-12): conteos; tras aplicar, recargar. Los KC salen del snapshot del recálculo (pueden ser 0 o «No calculable» si nadie está cerrado).

Opcional, 20 s: aplicar un seed sin cambiar el tipo y leer el aviso: «Este tipo no mueve el Gantt».

## Efectos (qué cambió y por qué)

Un solo `RecalculoService`. Nadie recalcula por su cuenta. Alta de proyecto no recalcula; estas tres acciones sí.

| # | Qué se tocó | Gantt | Costos | Línea base | KPI / tableros |
|---|---|---|---|---|---|
| 1 | 1.1.1 de 10 a 12 d | 1.1.1 y cadena FS (1.2.1…) | Costo plan igual; carga diaria baja | Intacta | Recalcular y recargar `/tableros` (C-12). Tablero de proyecto: B-21 |
| 2 | P2 en 1.1.2: 45→40 h | Fechas iguales | Costo 1.440.000 → 1.280.000; alerta se apaga | Intacta | Igual |
| 3 | Aplicar TIEMPO a 3.1.1 (+5 d) | 3.1.1 a 10 d; FS 3.1.2 y 3.2.1 | Costo plan igual | Intacta | Recargar `/tableros` (C-13 automatiza el refresco) |

Aprobar ≠ aplicar. Aplicar un no-TIEMPO no llama al recálculo de fechas.

## Si algo falla en vivo

- Seed a medias: reset con `down -v`.
- No aparece `EEC-2026`: `APP_SEED_ENABLED=true`.
- 403 al aprobar: no es ADMIN.
- Aplicar TIEMPO gris: falta tarea hoja.
- Gantt vacío: elegir el proyecto en el combo (`/gantt?id=`).
