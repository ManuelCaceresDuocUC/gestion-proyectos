// Análisis de una hoja de Excel: detecta columnas, jerarquía (EDT), duraciones y predecesoras.
// Es lógica pura (sin React ni red) para poder probarla aparte.
import type { TipoDependencia } from "../../types";
import type { Celda } from "./lectorXlsx";

export type CampoMapeo = "id" | "codigo" | "nombre" | "nivel" | "duracion" | "inicio" | "fin" | "predecesoras" | "avance";

/** Índice de columna por campo; null si no se usa. */
export type Mapeo = Record<CampoMapeo, number | null>;

export const ETIQUETA_CAMPO: Record<CampoMapeo, string> = {
  id: "Identificador (al que apuntan las predecesoras)",
  codigo: "Código EDT (1, 1.1, 1.1.1)",
  nombre: "Nombre de la tarea",
  nivel: "Nivel (1, 2, 3)",
  duracion: "Duración (días)",
  inicio: "Fecha de inicio",
  fin: "Fecha de fin",
  predecesoras: "Predecesoras",
  avance: "Avance (%)",
};

export const CAMPOS: CampoMapeo[] = [
  "nombre", "codigo", "nivel", "id", "duracion", "inicio", "fin", "predecesoras", "avance",
];

export const MAX_NIVELES = 3;

const SINONIMOS: Record<CampoMapeo, string[]> = {
  nombre: ["nombre", "nombre de tarea", "nombre tarea", "tarea", "actividad", "descripcion", "descripcion de la tarea",
    "task name", "task", "name", "nombre de la actividad", "entregable", "paquete de trabajo", "item"],
  codigo: ["codigo", "cod", "codigo edt", "edt", "wbs", "codigo wbs", "n edt", "numero edt", "nº edt"],
  nivel: ["nivel", "nivel edt", "nivel de esquema", "nivel de esquema", "level", "outline level"],
  id: ["id", "n", "no", "nro", "numero", "n°", "nº", "#", "identificador", "id tarea", "id de tarea"],
  duracion: ["duracion", "duracion dias", "dias", "duracion en dias", "duracion (dias)", "dur", "duration", "dias habiles",
    "dias laborales"],
  inicio: ["inicio", "fecha inicio", "fecha de inicio", "comienzo", "start", "fecha comienzo", "inicio plan", "inicio planificado"],
  fin: ["fin", "fecha fin", "fecha de fin", "termino", "término", "finalizacion", "end", "finish", "fecha termino",
    "fecha de termino", "fin plan", "fin planificado"],
  predecesoras: ["predecesoras", "predecesora", "predecesor", "predecesores", "dependencias", "dependencia", "depende de",
    "precedencias", "precedencia", "predecessors", "antecesoras"],
  avance: ["avance", "% avance", "avance %", "% completado", "porcentaje completado", "progreso", "% complete", "completado",
    "avance real"],
};

export function normalizar(texto: unknown): string {
  return String(texto ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/[_\-.:]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function normalizarSinonimo(texto: string): string {
  return normalizar(texto);
}

function textoCelda(c: Celda | undefined): string {
  if (c === null || c === undefined) return "";
  if (c instanceof Date) return fechaIso(c);
  return String(c).trim();
}

export function fechaIso(d: Date): string {
  return d.toISOString().slice(0, 10);
}

function vacia(fila: Celda[] | undefined): boolean {
  return !fila || fila.every((c) => textoCelda(c) === "");
}

function puntaje(fila: Celda[]): number {
  const conocidos = new Set<string>();
  for (const campo of CAMPOS) {
    for (const sin of SINONIMOS[campo]) conocidos.add(normalizarSinonimo(sin));
  }
  return fila.filter((c) => conocidos.has(normalizar(c))).length;
}

/** Fila de encabezados: la que más nombres de columna conocidos tiene entre las primeras 20. */
export function detectarEncabezado(filas: Celda[][]): number {
  let mejor = -1;
  let mejorPuntaje = 0;
  for (let i = 0; i < Math.min(filas.length, 20); i++) {
    const p = puntaje(filas[i] ?? []);
    if (p > mejorPuntaje) {
      mejor = i;
      mejorPuntaje = p;
    }
  }
  if (mejor >= 0) return mejor;
  const primera = filas.findIndex((f) => !vacia(f));
  return primera < 0 ? 0 : primera;
}

export function detectarMapeo(encabezados: Celda[]): Mapeo {
  const mapeo: Mapeo = {
    id: null, codigo: null, nombre: null, nivel: null, duracion: null, inicio: null, fin: null, predecesoras: null, avance: null,
  };
  const usadas = new Set<number>();
  // Primero coincidencia exacta; "nombre" antes que "id" para que "N°" no gane a la tarea.
  const orden: CampoMapeo[] = ["nombre", "predecesoras", "codigo", "nivel", "duracion", "inicio", "fin", "avance", "id"];
  for (const campo of orden) {
    const sins = SINONIMOS[campo].map(normalizarSinonimo);
    const idx = encabezados.findIndex((h, i) => !usadas.has(i) && sins.includes(normalizar(h)));
    if (idx >= 0) {
      mapeo[campo] = idx;
      usadas.add(idx);
    }
  }
  if (mapeo.nombre === null) {
    // Sin coincidencia exacta: primera columna cuyo encabezado contenga "nombre", "tarea" o "actividad".
    const idx = encabezados.findIndex((h, i) => !usadas.has(i) && /nombre|tarea|actividad/.test(normalizar(h)));
    if (idx >= 0) mapeo.nombre = idx;
  }
  return mapeo;
}

// ---------- valores ----------

const NUMERO = /-?\d+(?:[.,]\d+)?/;

export function numeroDe(c: Celda | undefined): number | null {
  if (typeof c === "number") return Number.isFinite(c) ? c : null;
  if (typeof c === "string") {
    const m = NUMERO.exec(c.replace(/\s/g, ""));
    if (m) return Number(m[0].replace(",", "."));
  }
  return null;
}

/** Días hábiles lunes–viernes entre dos fechas, ambas incluidas (mínimo 1). */
export function diasHabilesEntre(inicio: Date, fin: Date): number {
  let dias = 0;
  const d = new Date(Date.UTC(inicio.getUTCFullYear(), inicio.getUTCMonth(), inicio.getUTCDate()));
  const limite = Date.UTC(fin.getUTCFullYear(), fin.getUTCMonth(), fin.getUTCDate());
  while (d.getTime() <= limite && dias < 5000) {
    const dia = d.getUTCDay();
    if (dia !== 0 && dia !== 6) dias++;
    d.setUTCDate(d.getUTCDate() + 1);
  }
  return Math.max(1, dias);
}

function fechaDe(c: Celda | undefined): Date | null {
  if (c instanceof Date) return c;
  if (typeof c === "string") {
    const t = c.trim();
    let m = /^(\d{4})-(\d{2})-(\d{2})/.exec(t);
    if (m) return new Date(Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])));
    m = /^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})/.exec(t);
    if (m) {
      const anio = Number(m[3]) < 100 ? 2000 + Number(m[3]) : Number(m[3]);
      return new Date(Date.UTC(anio, Number(m[2]) - 1, Number(m[1])));
    }
  }
  return null;
}

// ---------- predecesoras ----------

export interface PredecesoraTexto {
  ref: string;
  tipo: TipoDependencia;
  desfase: number;
}

const TIPOS: Record<string, TipoDependencia> = {
  FS: "FS", FC: "FS", SS: "SS", CC: "SS", FF: "FF", SF: "SF", CF: "SF",
};

/** "3", "1.1.1", "3FS+2", "4 SS -1 días", "2;5CC" (también FC/CC/CF de MS Project en español). */
export function parsearPredecesoras(texto: string): PredecesoraTexto[] {
  const salida: PredecesoraTexto[] = [];
  for (const trozo of texto.split(/[;,]/)) {
    const t = trozo.trim();
    if (!t) continue;
    const m = /^(.+?)\s*(?:(FS|FC|SS|CC|FF|SF|CF)\s*)?(?:([+-])\s*(\d+)\s*(?:d|dias|días|day|days)?)?$/i.exec(t);
    if (!m) {
      salida.push({ ref: t, tipo: "FS", desfase: 0 });
      continue;
    }
    const desfase = m[3] ? (m[3] === "-" ? -1 : 1) * Number(m[4]) : 0;
    salida.push({ ref: m[1].trim(), tipo: m[2] ? TIPOS[m[2].toUpperCase()] : "FS", desfase });
  }
  return salida;
}

// ---------- jerarquía ----------

export interface NodoImportado {
  /** Número de fila en Excel (1-based) o 0 si es la etapa sintética. */
  fila: number;
  nombre: string;
  nivel: number;
  padre: number | null;
  hijos: number;
  duracion: number | null;
  avance: number | null;
  inicio: Date | null;
  predecesoras: PredecesoraTexto[];
  claves: string[];
  avisos: string[];
}

export interface DependenciaImportada {
  predecesora: number;
  sucesora: number;
  tipo: TipoDependencia;
  desfase: number;
}

export interface Analisis {
  nodos: NodoImportado[];
  dependencias: DependenciaImportada[];
  errores: string[];
  avisos: string[];
  jerarquia: "nivel" | "codigo" | "sangria" | "plana";
}

const CODIGO_EDT = /^\d+(?:\.\d+)*$/;

function nivelesPorSangria(nombres: string[]): number[] | null {
  const sangrias = nombres.map((n) => /^[\s ]*/.exec(n)?.[0].length ?? 0);
  const distintas = Array.from(new Set(sangrias)).sort((a, b) => a - b);
  if (distintas.length < 2) return null;
  return sangrias.map((s) => distintas.indexOf(s) + 1);
}

export function analizar(filas: Celda[][], encabezado: number, mapeo: Mapeo): Analisis {
  const errores: string[] = [];
  const avisos: string[] = [];
  const vacio: Analisis = { nodos: [], dependencias: [], errores, avisos, jerarquia: "plana" };
  if (mapeo.nombre === null) {
    errores.push("Indique qué columna contiene el nombre de la tarea.");
    return vacio;
  }
  const datos: Array<{ fila: number; celdas: Celda[] }> = [];
  for (let i = encabezado + 1; i < filas.length; i++) {
    const celdas = filas[i] ?? [];
    if (textoCelda(celdas[mapeo.nombre]) === "") {
      if (!vacia(celdas)) avisos.push(`Fila ${i + 1}: tiene datos pero no nombre; se omitió.`);
      continue;
    }
    datos.push({ fila: i + 1, celdas });
  }
  if (datos.length === 0) {
    errores.push("No se encontraron tareas bajo la fila de encabezados.");
    return vacio;
  }

  const crudos = datos.map((d) => String(d.celdas[mapeo.nombre as number] ?? ""));
  const nombres = crudos.map((n) => n.replace(/\s+/g, " ").trim());

  // Nivel de cada fila: columna Nivel, código EDT, sangría del nombre o lista plana.
  let niveles: number[] | null = null;
  let jerarquia: Analisis["jerarquia"] = "plana";
  if (mapeo.nivel !== null) {
    const vals = datos.map((d) => numeroDe(d.celdas[mapeo.nivel as number]));
    if (vals.every((v) => v !== null)) {
      const minimo = Math.min(...(vals as number[]));
      niveles = (vals as number[]).map((v) => Math.max(1, Math.round(v - minimo + 1)));
      jerarquia = "nivel";
    } else {
      avisos.push("La columna de nivel tiene celdas vacías o no numéricas; se probó con el código o la sangría.");
    }
  }
  if (!niveles && mapeo.codigo !== null) {
    const codigos = datos.map((d) => textoCelda(d.celdas[mapeo.codigo as number]).replace(/\.$/, ""));
    if (codigos.every((c) => CODIGO_EDT.test(c))) {
      niveles = codigos.map((c) => c.split(".").length);
      jerarquia = "codigo";
    } else {
      avisos.push("La columna de código no tiene el formato 1, 1.1, 1.1.1 en todas las filas; no se usó para la jerarquía.");
    }
  }
  if (!niveles) {
    const porSangria = nivelesPorSangria(crudos);
    if (porSangria) {
      niveles = porSangria;
      jerarquia = "sangria";
    }
  }

  const nodos: NodoImportado[] = [];
  const etapaSintetica = !niveles;
  if (etapaSintetica) {
    avisos.push(
      "No se pudo detectar la jerarquía (falta una columna de nivel o de código EDT). Todas las filas se importan como " +
        "actividades de una etapa llamada «Importado desde Excel».",
    );
    nodos.push({
      fila: 0, nombre: "Importado desde Excel", nivel: 1, padre: null, hijos: 0, duracion: null, avance: null, inicio: null,
      predecesoras: [], claves: [], avisos: [],
    });
  }

  const pila: number[] = [];
  datos.forEach((d, i) => {
    const nodoAvisos: string[] = [];
    let nivel = etapaSintetica ? 2 : (niveles as number[])[i];
    if (nivel > MAX_NIVELES) {
      nodoAvisos.push(`Nivel ${nivel}: el sistema admite ${MAX_NIVELES} niveles; se importa como subactividad.`);
      nivel = MAX_NIVELES;
    }
    while (pila.length > 0 && nodos[pila[pila.length - 1]].nivel >= nivel) pila.pop();
    let padre = pila.length > 0 ? pila[pila.length - 1] : null;
    if (etapaSintetica) padre = 0;
    if (padre === null && nivel > 1) {
      nodoAvisos.push(`Nivel ${nivel} sin un nivel superior antes; se importa como etapa.`);
      nivel = 1;
    } else if (padre !== null && nodos[padre].nivel < nivel - 1) {
      nodoAvisos.push(`Salto de nivel (de ${nodos[padre].nivel} a ${nivel}); se cuelga del nivel ${nodos[padre].nivel}.`);
    }
    const claves: string[] = [];
    if (mapeo.id !== null) claves.push(textoCelda(d.celdas[mapeo.id]).replace(/\.0$/, ""));
    if (mapeo.codigo !== null) claves.push(textoCelda(d.celdas[mapeo.codigo]).replace(/\.$/, ""));
    if (mapeo.id === null && mapeo.codigo === null) claves.push(String(i + 1));

    const inicio = mapeo.inicio !== null ? fechaDe(d.celdas[mapeo.inicio]) : null;
    const fin = mapeo.fin !== null ? fechaDe(d.celdas[mapeo.fin]) : null;
    let duracion = mapeo.duracion !== null ? numeroDe(d.celdas[mapeo.duracion]) : null;
    if (duracion !== null) {
      if (duracion < 0) {
        nodoAvisos.push("Duración negativa; se usó 0.");
        duracion = 0;
      }
      if (!Number.isInteger(duracion)) {
        nodoAvisos.push(`Duración ${duracion} redondeada hacia arriba.`);
        duracion = Math.ceil(duracion);
      }
    } else if (inicio && fin && fin >= inicio) {
      duracion = diasHabilesEntre(inicio, fin);
    }
    let avance = mapeo.avance !== null ? numeroDe(d.celdas[mapeo.avance]) : null;
    if (avance !== null) avance = Math.min(100, Math.max(0, avance));

    nodos.push({
      fila: d.fila, nombre: nombres[i].slice(0, 200), nivel, padre, hijos: 0, duracion, avance, inicio,
      predecesoras: mapeo.predecesoras !== null ? parsearPredecesoras(textoCelda(d.celdas[mapeo.predecesoras])) : [],
      claves: claves.filter((c) => c !== ""), avisos: nodoAvisos,
    });
    pila.push(nodos.length - 1);
  });

  // Avance de los porcentajes dados como fracción (0–1).
  const avances = nodos.map((n) => n.avance).filter((a): a is number => a !== null);
  if (avances.length > 0 && avances.every((a) => a <= 1) && avances.some((a) => !Number.isInteger(a))) {
    nodos.forEach((n) => { if (n.avance !== null) n.avance = Math.round(n.avance * 100); });
  }

  nodos.forEach((n) => { if (n.padre !== null) nodos[n.padre].hijos++; });
  nodos.forEach((n) => {
    if (n.hijos > 0) {
      if (n.duracion !== null) n.avisos.push("Tiene subtareas: su duración y avance se calculan desde ellas.");
      n.duracion = null;
      n.avance = null;
    } else if (n.fila > 0 && n.duracion === null) {
      n.avisos.push("Sin duración; se usa 1 día hábil.");
    }
  });

  // Predecesoras: se resuelven por identificador, luego por código; solo entre hojas.
  const porClave = new Map<string, number>();
  nodos.forEach((n, i) => n.claves.forEach((c) => { if (!porClave.has(c)) porClave.set(c, i); }));
  const dependencias: DependenciaImportada[] = [];
  const vistas = new Set<string>();
  nodos.forEach((suc, iSuc) => {
    for (const p of suc.predecesoras) {
      const iPred = porClave.get(p.ref);
      if (iPred === undefined) {
        suc.avisos.push(`Predecesora «${p.ref}» no encontrada; se omitió.`);
        continue;
      }
      if (iPred === iSuc) {
        suc.avisos.push("Se nombra a sí misma como predecesora; se omitió.");
        continue;
      }
      if (nodos[iPred].hijos > 0 || suc.hijos > 0) {
        suc.avisos.push(`Predecesora «${p.ref}»: las dependencias solo van entre subtareas finales; se omitió.`);
        continue;
      }
      const clave = `${iPred}>${iSuc}`;
      if (vistas.has(clave)) continue;
      vistas.add(clave);
      dependencias.push({ predecesora: iPred, sucesora: iSuc, tipo: p.tipo, desfase: p.desfase });
    }
  });

  return { nodos, dependencias, errores, avisos, jerarquia };
}

export function tipoDeNivel(nivel: number): string {
  return nivel === 1 ? "Etapa" : nivel === 2 ? "Actividad" : "Subactividad";
}

/** Fecha de inicio más temprana de las filas, para sugerir la del proyecto. */
export function inicioSugerido(nodos: NodoImportado[]): string {
  const fechas = nodos.map((n) => n.inicio).filter((d): d is Date => d !== null);
  if (fechas.length === 0) return "";
  return fechaIso(new Date(Math.min(...fechas.map((d) => d.getTime()))));
}
