import type { FilaComparacion } from "../../types/gantt";
import type { NodoEdt } from "../../types/planificacion";

/** Una fila visible del Gantt: el nodo de la EDT más su comparación contra la base. */
export interface FilaGantt {
  nodo: NodoEdt;
  profundidad: number;
  comp: FilaComparacion | undefined;
  /** Tiene subtareas (se dibuja como barra resumen y se puede plegar). */
  esResumen: boolean;
  /** Duración 0 en una tarea sin hijos. */
  esHito: boolean;
}

export function aplanar(
  nodos: NodoEdt[],
  comparacion: Map<number, FilaComparacion>,
  plegados: Set<number>,
  profundidad = 0,
): FilaGantt[] {
  const filas: FilaGantt[] = [];
  for (const n of nodos) {
    const esResumen = n.hijos.length > 0;
    filas.push({
      nodo: n,
      profundidad,
      comp: comparacion.get(n.id),
      esResumen,
      esHito: !esResumen && n.duracionDias === 0,
    });
    if (esResumen && !plegados.has(n.id)) {
      filas.push(...aplanar(n.hijos, comparacion, plegados, profundidad + 1));
    }
  }
  return filas;
}

export function buscarNodo(nodos: NodoEdt[], id: number): NodoEdt | undefined {
  for (const n of nodos) {
    if (n.id === id) return n;
    const dentro = buscarNodo(n.hijos, id);
    if (dentro) return dentro;
  }
  return undefined;
}
