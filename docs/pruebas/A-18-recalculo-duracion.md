# A-18 · Recálculo 1: cambiar una duración

Corresponde al primer recálculo de la demo. La parte de planificación está automatizada en
`RecalculoDuracionE2ETest`; los efectos en costos, KPI y tableros los completan B-18 y C-14 con sus módulos.

## Datos de entrada
Proyecto con inicio lunes 2-nov-2026, una etapa y cinco tareas (días hábiles):

| Código | Tarea | Duración | Dependencia |
|---|---|---|---|
| 1.1 | A | 5 | — |
| 1.2 | B | 2 | FS desde A |
| 1.3 | C | 3 | SS desde A, desfase +1 |
| 1.4 | D | 4 | FF desde A |
| 1.5 | E | 3 | SF desde A |

Se fija la línea base (v1) y luego se cambia la duración de A de 5 a 7 días (Gantt: clic en la barra de A, campo
"Duración", "Aplicar y recalcular"; o `PATCH /api/tareas/{id}` con `{"duracionDias": 7}`).

## Resultado esperado
| Tarea | Desviación inicio | Desviación fin | Por qué |
|---|---|---|---|
| A (1.1) | 0 | +2 | Dura 2 días más |
| B (1.2) | +2 | +2 | FS: empieza cuando A termina |
| C (1.3) | 0 | 0 | SS: depende del inicio de A |
| D (1.4) | — | +2 | FF: termina cuando A termina |
| E (1.5) | — | 0 | SF: depende del inicio de A |
| Proyecto | — | +2 | El fin lo marca B |

- La línea base no cambia (`finBase` igual antes y después).
- El panel del Gantt lista las tareas cuyas fechas cambiaron: A, B y D.
- Volver a 5 días deja todas las desviaciones en 0.
- Crear una dependencia B → A (FS) responde 409 `CICLO_EN_DEPENDENCIAS` y no cambia ninguna fecha.

## Resultado obtenido
_Completar tras ejecutar `mvn test` (clase `RecalculoDuracionE2ETest`) y la prueba manual en el Gantt._

| Prueba | Fecha | Resultado | Responsable |
|---|---|---|---|
| Automatizada | | | |
| Manual en el Gantt (capturas antes/después en `docs/pruebas/capturas/`) | | | |
