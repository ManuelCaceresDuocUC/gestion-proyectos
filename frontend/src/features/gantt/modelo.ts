import type { FilaComparacion } from "../../types/gantt";
import type { Hito } from "../../types/hitos";
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

/** Renglón del Gantt: una fila de la EDT o un hito registrado, en el orden en que se dibujan. */
export type Renglon =
  | { tipo: "tarea"; fila: FilaGantt; indice: number }
  | { tipo: "hito"; hito: Hito };

/** Código de la etapa de un hito: la de su actividad que termina más tarde (el hito cierra ese trabajo). */
function etapaDeHito(h: Hito): string | null {
  let elegida = h.actividades[0];
  for (const a of h.actividades) {
    if (!elegida || (a.finPlan ?? "") >= (elegida.finPlan ?? "")) elegida = a;
  }
  return elegida ? elegida.codigo.split(".")[0] : null;
}

/** Intercala cada hito justo después de la última fila de su etapa; si no tiene etapa, va al final. */
export function disponer(filas: FilaGantt[], hitos: Hito[]): Renglon[] {
  const tras = new Map<number, Hito[]>();
  const alFinal: Hito[] = [];
  for (const h of [...hitos].sort((a, b) => (a.fechaPlan ?? "").localeCompare(b.fechaPlan ?? ""))) {
    const etapa = etapaDeHito(h);
    let destino = -1;
    if (etapa) {
      filas.forEach((f, i) => {
        if (f.nodo.codigo === etapa || f.nodo.codigo.startsWith(`${etapa}.`)) destino = i;
      });
    }
    if (destino < 0) { alFinal.push(h); continue; }
    tras.set(destino, [...(tras.get(destino) ?? []), h]);
  }
  const salida: Renglon[] = [];
  filas.forEach((fila, indice) => {
    salida.push({ tipo: "tarea", fila, indice });
    for (const hito of tras.get(indice) ?? []) salida.push({ tipo: "hito", hito });
  });
  for (const hito of alFinal) salida.push({ tipo: "hito", hito });
  return salida;
}
