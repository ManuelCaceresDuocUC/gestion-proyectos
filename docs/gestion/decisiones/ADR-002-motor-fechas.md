# ADR-002: Semántica del motor de fechas y dependencias

- **Estado:** aceptado (A-07, A-08)
- **Contexto:** el Gantt exige los 4 tipos de dependencia (FS, SS, FF, SF) con desfases, y que al cambiar una duración o una precedencia se recalculen las fechas de todo el proyecto.
- **Decisión:**
  - Las duraciones son en **días hábiles** (lunes a viernes menos feriados). El día de término es **inclusivo**: `fin = inicio + (duración - 1)` días hábiles. Duración 0 = hito (`inicio = fin`).
  - El **desfase** (lag) también es en días hábiles; negativo = adelanto.
  - Reglas, con `P` = predecesora y `S` = sucesora:
    - **FS:** `inicio(S) >= fin(P) + 1 + lag` días hábiles (con lag 0, empieza el día hábil siguiente).
    - **SS:** `inicio(S) >= inicio(P) + lag`.
    - **FF:** `fin(S) >= fin(P) + lag`.
    - **SF:** `fin(S) >= inicio(P) + lag`.
  - Con varias predecesoras gana la más restrictiva (la fecha más tardía). Sin predecesoras, la tarea empieza en la fecha de inicio del proyecto (o en su inicio mínimo si es posterior).
  - Ninguna tarea empieza antes del inicio del proyecto, aunque un SF o un adelanto lo pidieran.
  - Cálculo en un solo paso hacia adelante (orden topológico). Antes se valida que no haya ciclos (algoritmo de Kahn) y se rechaza una dependencia que apunte a una tarea inexistente.
  - Las etapas y actividades con hijos **no entran al motor**: sus fechas se consolidan desde las hojas (A-06).
- **Alternativas consideradas:** duración en días corridos (descartada: no refleja fines de semana); fin exclusivo (descartada: confunde en el Gantt); paso hacia atrás para holguras y ruta crítica (fuera del alcance de la rúbrica).
- **Consecuencias:** el motor es dominio puro, testeable sin base de datos; el calendario por proyecto (`dias_laborales`) aún no se usa, por ahora todos usan lunes a viernes.
