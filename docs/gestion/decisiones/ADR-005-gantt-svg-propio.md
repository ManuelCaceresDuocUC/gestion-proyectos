# ADR-005 · Gantt propio en SVG (sin librería)

- **Estado:** aceptada (decisión en chat, 24 h de silencio = aprobada)
- **Contexto:** A-15, A-16 y A-17 piden barras jerárquicas, avance, flechas de los 4 tipos de dependencia (FS, SS, FF, SF), escala por día y semana, hitos, barra de línea base superpuesta, atrasos resaltados y marcador de hoy.
- **Opciones:** (1) librería de Gantt (gantt-task-react, frappe-gantt, dhtmlx); (2) SVG propio con React.
- **Decisión:** SVG propio (`frontend/src/features/gantt`).
- **Razones:**
  - Las librerías dibujan flechas solo de fin a inicio o con comportamiento propio; SS, FF y SF y la barra de línea base superpuesta exigen parchear o salirse de la librería.
  - Una librería con estilos y modelo de datos propios obliga a transformar nuestros datos y a pelear con su CSS; un SVG de ~200 líneas se entiende y se defiende en la demo.
  - Sin dependencia nueva: menos riesgo en el CI y en el build de Docker.
- **Consecuencias:** no hay arrastrar y soltar de barras; la edición (duración, tipo y desfase de dependencias) se hace en un panel y el backend recalcula (único punto: `RecalculoService`). Escala por día y semana; no hay escala mensual.
- **Fuente de datos:** `GET /edt` (jerarquía, duración, fechas plan), `GET /comparacion` (base, real, atraso) y `GET /dependencias`.
