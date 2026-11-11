// Carga un análisis de Excel en el sistema usando las APIs existentes: proyecto, EDT y dependencias.
import { dependenciasApi, edtApi, proyectosApi } from "../../api/planificacion";
import type { ProyectoDatos } from "../../types/planificacion";
import type { Analisis } from "./analisis";

export interface ResultadoCarga {
  proyectoId: number;
  codigo: string;
  etapas: number;
  actividades: number;
  subactividades: number;
  dependencias: number;
  fallos: string[];
}

function mensaje(error: unknown): string {
  return error instanceof Error ? error.message : "error desconocido";
}

/**
 * Orden: proyecto, luego cada fila (los superiores siempre van antes que sus hijos), luego las dependencias,
 * cuando ya existen todas las tareas. Cada paso recalcula el proyecto en el servidor. Un fallo en una fila o
 * dependencia se anota y la carga sigue; un fallo al crear el proyecto la detiene.
 */
export async function cargar(
  analisis: Analisis,
  datos: ProyectoDatos,
  progreso: (hecho: number, total: number, texto: string) => void,
): Promise<ResultadoCarga> {
  const total = analisis.nodos.length + analisis.dependencias.length;
  let hecho = 0;
  progreso(hecho, total, "Creando el proyecto…");
  const proyecto = await proyectosApi.crear(datos);

  const resultado: ResultadoCarga = {
    proyectoId: proyecto.id,
    codigo: proyecto.codigo,
    etapas: 0,
    actividades: 0,
    subactividades: 0,
    dependencias: 0,
    fallos: [],
  };
  const ids = new Map<number, number>();

  for (let i = 0; i < analisis.nodos.length; i++) {
    const nodo = analisis.nodos[i];
    const etiqueta = nodo.fila > 0 ? `Fila ${nodo.fila} «${nodo.nombre}»` : `«${nodo.nombre}»`;
    progreso(hecho, total, `Creando tareas (${i + 1} de ${analisis.nodos.length})…`);
    hecho++;
    let parentId: number | null = null;
    if (nodo.padre !== null) {
      const idPadre = ids.get(nodo.padre);
      if (idPadre === undefined) {
        resultado.fallos.push(`${etiqueta}: no se creó porque falló su nivel superior.`);
        continue;
      }
      parentId = idPadre;
    }
    const esHoja = nodo.hijos === 0;
    try {
      const creada = await edtApi.crear(proyecto.id, {
        parentId,
        nombre: nodo.nombre,
        descripcion: null,
        responsableId: null,
        duracionDias: esHoja && nodo.duracion !== null ? nodo.duracion : undefined,
        avance: esHoja && nodo.avance !== null ? nodo.avance : undefined,
      });
      ids.set(i, creada.id);
      if (creada.tipo === "ETAPA") resultado.etapas++;
      else if (creada.tipo === "ACTIVIDAD") resultado.actividades++;
      else resultado.subactividades++;
    } catch (error) {
      resultado.fallos.push(`${etiqueta}: ${mensaje(error)}`);
    }
  }

  for (let i = 0; i < analisis.dependencias.length; i++) {
    const d = analisis.dependencias[i];
    progreso(hecho, total, `Creando dependencias (${i + 1} de ${analisis.dependencias.length})…`);
    hecho++;
    const pred = ids.get(d.predecesora);
    const suc = ids.get(d.sucesora);
    const nombres = `«${analisis.nodos[d.predecesora].nombre}» → «${analisis.nodos[d.sucesora].nombre}»`;
    if (pred === undefined || suc === undefined) {
      resultado.fallos.push(`Dependencia ${nombres}: se omitió porque falta una de las tareas.`);
      continue;
    }
    try {
      await dependenciasApi.crear(proyecto.id, pred, suc, d.tipo, d.desfase);
      resultado.dependencias++;
    } catch (error) {
      resultado.fallos.push(`Dependencia ${nombres}: ${mensaje(error)}`);
    }
  }
  progreso(total, total, "Listo.");
  return resultado;
}
