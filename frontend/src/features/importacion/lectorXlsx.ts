// Lector mínimo de .xlsx sin dependencias: un .xlsx es un zip con XML. Se descomprime con
// DecompressionStream (navegadores actuales) y se leen hojas, textos compartidos y fechas.

export type Celda = string | number | boolean | Date | null;

export interface Hoja {
  nombre: string;
  filas: Celda[][];
}

const MAX_FILAS = 5000;
const MAX_COLUMNAS = 200;

interface EntradaZip {
  nombre: string;
  metodo: number;
  tamComprimido: number;
  posicionLocal: number;
}

function leerZip(datos: Uint8Array): Map<string, EntradaZip> {
  const vista = new DataView(datos.buffer, datos.byteOffset, datos.byteLength);
  let fin = -1;
  for (let i = datos.length - 22; i >= Math.max(0, datos.length - 65_557); i--) {
    if (vista.getUint32(i, true) === 0x06054b50) {
      fin = i;
      break;
    }
  }
  if (fin < 0) {
    throw new Error("El archivo no es un .xlsx válido (no se encontró el índice del zip).");
  }
  const total = vista.getUint16(fin + 10, true);
  let pos = vista.getUint32(fin + 16, true);
  const decodificador = new TextDecoder("utf-8");
  const entradas = new Map<string, EntradaZip>();
  for (let n = 0; n < total; n++) {
    if (vista.getUint32(pos, true) !== 0x02014b50) {
      throw new Error("El archivo .xlsx está dañado.");
    }
    const metodo = vista.getUint16(pos + 10, true);
    const tamComprimido = vista.getUint32(pos + 20, true);
    const largoNombre = vista.getUint16(pos + 28, true);
    const largoExtra = vista.getUint16(pos + 30, true);
    const largoComentario = vista.getUint16(pos + 32, true);
    const posicionLocal = vista.getUint32(pos + 42, true);
    const nombre = decodificador.decode(datos.subarray(pos + 46, pos + 46 + largoNombre));
    entradas.set(nombre, { nombre, metodo, tamComprimido, posicionLocal });
    pos += 46 + largoNombre + largoExtra + largoComentario;
  }
  return entradas;
}

async function extraer(datos: Uint8Array, entrada: EntradaZip): Promise<string> {
  const vista = new DataView(datos.buffer, datos.byteOffset, datos.byteLength);
  const p = entrada.posicionLocal;
  if (vista.getUint32(p, true) !== 0x04034b50) {
    throw new Error("El archivo .xlsx está dañado.");
  }
  const inicio = p + 30 + vista.getUint16(p + 26, true) + vista.getUint16(p + 28, true);
  const bruto = datos.slice(inicio, inicio + entrada.tamComprimido);
  if (entrada.metodo === 0) {
    return new TextDecoder("utf-8").decode(bruto);
  }
  if (entrada.metodo !== 8) {
    throw new Error("El .xlsx usa una compresión que no se puede leer.");
  }
  const Descompresor = (globalThis as unknown as {
    DecompressionStream?: new (formato: string) => ReadableWritablePair<Uint8Array, Uint8Array>;
  })
    .DecompressionStream;
  if (!Descompresor) {
    throw new Error("Este navegador no puede abrir .xlsx. Use una versión reciente de Chrome, Edge, Firefox o Safari.");
  }
  const flujo = new Blob([bruto]).stream().pipeThrough(new Descompresor("deflate-raw"));
  const buffer = await new Response(flujo).arrayBuffer();
  return new TextDecoder("utf-8").decode(buffer);
}

function xml(texto: string): Document {
  const doc = new DOMParser().parseFromString(texto, "application/xml");
  if (doc.getElementsByTagName("parsererror").length > 0) {
    throw new Error("El archivo .xlsx tiene un XML interno inválido.");
  }
  return doc;
}

function textoDe(elemento: Element): string {
  // <si><t>…</t></si> o texto enriquecido <r><t>…</t></r>; se ignora la fonética <rPh>.
  let salida = "";
  const recorrer = (nodo: Element) => {
    for (const hijo of Array.from(nodo.children)) {
      if (hijo.tagName === "rPh") continue;
      if (hijo.tagName === "t") salida += hijo.textContent ?? "";
      else recorrer(hijo);
    }
  };
  recorrer(elemento);
  return salida;
}

function columnaDe(ref: string): number {
  let n = 0;
  for (const c of ref) {
    const k = c.charCodeAt(0);
    if (k < 65 || k > 90) break;
    n = n * 26 + (k - 64);
  }
  return n - 1;
}

// Formatos de fecha incorporados en Excel (ids 14–22 y 45–47) y detección en formatos propios.
const FORMATOS_FECHA = new Set([14, 15, 16, 17, 18, 19, 20, 21, 22, 45, 46, 47]);

function esFormatoFecha(codigo: string): boolean {
  const limpio = codigo.replace(/"[^"]*"/g, "").replace(/\[[^\]]*\]/g, "").replace(/\\./g, "");
  return /[dmyhs]/i.test(limpio) && !/^(general|0|#)/i.test(limpio.trim());
}

function estilosConFecha(textoStyles: string | null): Set<number> {
  const fechas = new Set<number>();
  if (!textoStyles) return fechas;
  const doc = xml(textoStyles);
  const formatos = new Map<number, string>();
  for (const f of Array.from(doc.getElementsByTagName("numFmt"))) {
    formatos.set(Number(f.getAttribute("numFmtId")), f.getAttribute("formatCode") ?? "");
  }
  const celdaXfs = doc.getElementsByTagName("cellXfs")[0];
  if (!celdaXfs) return fechas;
  Array.from(celdaXfs.getElementsByTagName("xf")).forEach((xf, indice) => {
    const id = Number(xf.getAttribute("numFmtId") ?? "0");
    const codigo = formatos.get(id);
    if (FORMATOS_FECHA.has(id) || (codigo !== undefined && esFormatoFecha(codigo))) {
      fechas.add(indice);
    }
  });
  return fechas;
}

function serialAFecha(serial: number): Date {
  return new Date(Math.round((serial - 25569) * 86_400_000));
}

function leerHoja(textoHoja: string, compartidos: string[], conFecha: Set<number>): Celda[][] {
  const doc = xml(textoHoja);
  const filas: Celda[][] = [];
  for (const fila of Array.from(doc.getElementsByTagName("row"))) {
    const numero = Number(fila.getAttribute("r") ?? filas.length + 1);
    if (numero > MAX_FILAS) break;
    const celdas: Celda[] = [];
    for (const c of Array.from(fila.getElementsByTagName("c"))) {
      const ref = c.getAttribute("r");
      const col = ref ? columnaDe(ref) : celdas.length;
      if (col < 0 || col >= MAX_COLUMNAS) continue;
      const tipo = c.getAttribute("t");
      const v = c.getElementsByTagName("v")[0]?.textContent ?? null;
      let valor: Celda = null;
      if (tipo === "s") {
        valor = v === null ? null : (compartidos[Number(v)] ?? null);
      } else if (tipo === "inlineStr") {
        const inline = c.getElementsByTagName("is")[0];
        valor = inline ? textoDe(inline) : null;
      } else if (tipo === "str") {
        valor = v;
      } else if (tipo === "b") {
        valor = v === "1";
      } else if (tipo === "e") {
        valor = null;
      } else if (v !== null && v !== "") {
        const n = Number(v);
        valor = Number.isNaN(n) ? v : conFecha.has(Number(c.getAttribute("s") ?? "-1")) ? serialAFecha(n) : n;
      }
      while (celdas.length < col) celdas.push(null);
      celdas[col] = valor;
    }
    while (filas.length < numero - 1) filas.push([]);
    filas[numero - 1] = celdas;
  }
  return filas;
}

/** Lee todas las hojas de un .xlsx. Las filas vacías se conservan para mantener el número de fila de Excel. */
export async function leerXlsx(archivo: Blob): Promise<Hoja[]> {
  const datos = new Uint8Array(await archivo.arrayBuffer());
  const entradas = leerZip(datos);
  const leer = async (nombre: string): Promise<string | null> => {
    const entrada = entradas.get(nombre);
    return entrada ? extraer(datos, entrada) : null;
  };
  const libro = await leer("xl/workbook.xml");
  if (!libro) {
    throw new Error("El archivo no parece un libro de Excel (.xlsx). Si es .xls o .csv, guárdelo como .xlsx.");
  }
  const rels = await leer("xl/_rels/workbook.xml.rels");
  const destinos = new Map<string, string>();
  if (rels) {
    for (const r of Array.from(xml(rels).getElementsByTagName("Relationship"))) {
      const id = r.getAttribute("Id");
      const destino = r.getAttribute("Target");
      if (id && destino) {
        destinos.set(id, destino.startsWith("/") ? destino.slice(1) : `xl/${destino}`);
      }
    }
  }
  const textoCompartidos = await leer("xl/sharedStrings.xml");
  const compartidos = textoCompartidos
    ? Array.from(xml(textoCompartidos).getElementsByTagName("si")).map(textoDe)
    : [];
  const conFecha = estilosConFecha(await leer("xl/styles.xml"));

  const hojas: Hoja[] = [];
  const definidas = Array.from(xml(libro).getElementsByTagName("sheet"));
  for (let i = 0; i < definidas.length; i++) {
    const def = definidas[i];
    const id = def.getAttribute("r:id");
    const ruta = (id && destinos.get(id)) || `xl/worksheets/sheet${i + 1}.xml`;
    const texto = await leer(ruta);
    if (texto === null) continue;
    hojas.push({ nombre: def.getAttribute("name") ?? `Hoja ${i + 1}`, filas: leerHoja(texto, compartidos, conFecha) });
  }
  if (hojas.length === 0) {
    throw new Error("El libro no tiene hojas con datos.");
  }
  return hojas;
}
