# Importar un proyecto desde Excel

Pantalla **Importar Excel** (menú Planificación, ruta `/importar`). Sirve para migrar un plan que ya existe en una
hoja de cálculo: crea un **proyecto nuevo** con su EDT, duraciones, avance y dependencias. No modifica proyectos
existentes.

## Cómo se usa

1. Elegir un archivo `.xlsx` (hay un ejemplo en `docs/ejemplos/plan-ejemplo-importacion.xlsx`).
2. Revisar las columnas detectadas y corregirlas si hace falta (la fila de encabezados también se puede cambiar).
3. Completar código, nombre, fecha de inicio y presupuesto del proyecto. La fecha de inicio se sugiere desde la columna
   de inicio, si existe. Sin fecha no se calculan fechas ni se dibuja el Gantt.
4. Revisar la vista previa (tipo de cada fila, duración, predecesoras, avisos) y pulsar **Importar proyecto**.
5. El resultado enlaza al Gantt y a la EDT. **Deshacer importación** borra el proyecto recién creado.

## Qué reconoce

| Dato | Cómo se detecta |
|---|---|
| Nombre de la tarea | Columna «Tarea», «Nombre», «Actividad», «Nombre de tarea»… (obligatorio) |
| Jerarquía | Columna de **código EDT** (1, 1.1, 1.1.1), columna de **nivel**, o **sangría** del nombre. Sin ninguna, todas las filas pasan a una etapa «Importado desde Excel» |
| Duración | Días (`5`, `5 días`); si falta, se calcula con inicio y fin (lunes a viernes); si tampoco hay, 1 día |
| Predecesoras | `3`, `1.1.1`, `3FS+2`, `4SS-1`, varias separadas por `;` o `,` (también FC, CC y CF de MS Project en español). Se buscan por identificador o por código EDT |
| Avance | Porcentaje (`50`, `50%` o `0,5`) en las tareas finales |
| Fechas | Se usa la de inicio más temprana como inicio del proyecto |

## Reglas y límites

- Solo `.xlsx`. Un `.xls` o `.csv` se guarda antes como `.xlsx`.
- El sistema admite **3 niveles**; lo que esté más abajo se importa como subactividad (con aviso).
- Las duraciones, el avance y las dependencias solo van en las **tareas finales**; en las que tienen subtareas se
  calculan. Una predecesora que apunta a una tarea con subtareas se omite con aviso.
- Una predecesora que no se encuentra, o que forma un ciclo, se omite o se informa; el resto se importa.
- No se importan todavía recursos, costos, hitos ni solicitudes de cambio.

## Cómo funciona

Todo ocurre en el navegador: el `.xlsx` se lee sin librerías externas (es un zip con XML) y la carga usa las API que ya
existen (`POST /api/proyectos`, `POST /api/proyectos/{id}/tareas`, `POST /api/proyectos/{id}/dependencias`). Cada paso
recalcula el proyecto en el servidor, igual que si se hubiera cargado a mano. Código en
`frontend/src/features/importacion/` (`lectorXlsx.ts`, `analisis.ts`, `cargar.ts`, `ImportarPagina.tsx`).
