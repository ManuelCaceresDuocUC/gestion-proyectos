import { useMemo, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link } from "react-router-dom";
import { proyectosApi } from "../../api/planificacion";
import { mensajeDe } from "../proyectos/formato";
import {
  CAMPOS, ETIQUETA_CAMPO, analizar, detectarEncabezado, detectarMapeo, inicioSugerido, tipoDeNivel,
} from "./analisis";
import type { CampoMapeo, Mapeo } from "./analisis";
import { cargar } from "./cargar";
import type { ResultadoCarga } from "./cargar";
import { leerXlsx } from "./lectorXlsx";
import type { Celda, Hoja } from "./lectorXlsx";
import "./importacion.css";

const MAX_PREVIA = 150;

function letra(indice: number): string {
  let n = indice;
  let s = "";
  do {
    s = String.fromCharCode(65 + (n % 26)) + s;
    n = Math.floor(n / 26) - 1;
  } while (n >= 0);
  return s;
}

function texto(celda: Celda | undefined): string {
  if (celda === null || celda === undefined) return "";
  if (celda instanceof Date) return celda.toISOString().slice(0, 10);
  return String(celda).trim();
}

function codigoSugerido(nombreArchivo: string): string {
  const base = nombreArchivo.replace(/\.[^.]+$/, "").toUpperCase().replace(/[^A-Z0-9]+/g, "-").replace(/^-+|-+$/g, "");
  return (base || "IMPORTADO").slice(0, 30);
}

/** Importar un proyecto desde un Excel (.xlsx): detecta columnas, muestra una vista previa y lo carga. */
export default function ImportarPagina() {
  const [nombreArchivo, setNombreArchivo] = useState("");
  const [hojas, setHojas] = useState<Hoja[]>([]);
  const [hojaIdx, setHojaIdx] = useState(0);
  const [encabezado, setEncabezado] = useState(0);
  const [mapeo, setMapeo] = useState<Mapeo | null>(null);
  const [codigo, setCodigo] = useState("");
  const [nombre, setNombre] = useState("");
  const [inicio, setInicio] = useState("");
  const [presupuesto, setPresupuesto] = useState("0");
  const [leyendo, setLeyendo] = useState(false);
  const [importando, setImportando] = useState(false);
  const [avance, setAvance] = useState({ hecho: 0, total: 1, texto: "" });
  const [resultado, setResultado] = useState<ResultadoCarga | null>(null);
  const [confirmaBorrado, setConfirmaBorrado] = useState(false);
  const [error, setError] = useState("");

  const filas = hojas[hojaIdx]?.filas ?? [];
  const analisis = useMemo(
    () => (mapeo && filas.length > 0 ? analizar(filas, encabezado, mapeo) : null),
    [filas, encabezado, mapeo],
  );
  const titulos: Celda[] = filas[encabezado] ?? [];
  const columnas = Math.max(titulos.length, ...filas.slice(0, 30).map((f) => f.length));

  function prepararHoja(lista: Hoja[], indice: number, archivo: string) {
    const hoja = lista[indice];
    const enc = detectarEncabezado(hoja.filas);
    const detectado = detectarMapeo(hoja.filas[enc] ?? []);
    setHojaIdx(indice);
    setEncabezado(enc);
    setMapeo(detectado);
    const prueba = analizar(hoja.filas, enc, detectado);
    setInicio(inicioSugerido(prueba.nodos));
    if (!nombre) setNombre(archivo.replace(/\.[^.]+$/, ""));
  }

  async function alElegirArchivo(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0];
    if (!archivo) return;
    setError("");
    setResultado(null);
    setConfirmaBorrado(false);
    setLeyendo(true);
    try {
      const lista = await leerXlsx(archivo);
      setNombreArchivo(archivo.name);
      setHojas(lista);
      setCodigo(codigoSugerido(archivo.name));
      setNombre(archivo.name.replace(/\.[^.]+$/, ""));
      const primera = Math.max(0, lista.findIndex((h) => h.filas.length > 0));
      prepararHoja(lista, primera, archivo.name);
    } catch (err) {
      setHojas([]);
      setMapeo(null);
      setNombreArchivo("");
      setError(mensajeDe(err));
    } finally {
      setLeyendo(false);
      e.target.value = "";
    }
  }

  function cambiarCampo(campo: CampoMapeo, valor: string) {
    if (!mapeo) return;
    setMapeo({ ...mapeo, [campo]: valor === "" ? null : Number(valor) });
  }

  function cambiarEncabezado(valor: string) {
    const n = Math.max(1, Number(valor) || 1) - 1;
    setEncabezado(n);
    setMapeo(detectarMapeo(filas[n] ?? []));
  }

  async function importar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!analisis || analisis.errores.length > 0) return;
    setError("");
    setImportando(true);
    setResultado(null);
    setConfirmaBorrado(false);
    try {
      const res = await cargar(
        analisis,
        {
          codigo: codigo.trim(),
          nombre: nombre.trim(),
          descripcion: `Importado desde ${nombreArchivo}`,
          estado: "PLANIFICADO",
          responsableId: null,
          presupuesto: Number(presupuesto) || 0,
          moneda: "CLP",
          diasLaborales: "LMXJV",
          fechaInicio: inicio || null,
        },
        (hecho, total, textoAvance) => setAvance({ hecho, total, texto: textoAvance }),
      );
      setResultado(res);
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setImportando(false);
    }
  }

  async function deshacer() {
    if (!resultado) return;
    try {
      await proyectosApi.eliminar(resultado.proyectoId);
      setResultado(null);
      setConfirmaBorrado(false);
    } catch (err) {
      setError(mensajeDe(err));
    }
  }

  const etapas = analisis?.nodos.filter((n) => n.nivel === 1).length ?? 0;
  const hojasFinales = analisis?.nodos.filter((n) => n.hijos === 0).length ?? 0;
  const avisosFila = analisis?.nodos.reduce((suma, n) => suma + n.avisos.length, 0) ?? 0;
  const puedeImportar =
    !!analisis && analisis.errores.length === 0 && codigo.trim() !== "" && nombre.trim() !== "" && !importando;

  return (
    <section>
      <div className="encabezado">
        <h2>Importar desde Excel</h2>
      </div>
      <p className="vacio">
        Cargue un Excel (.xlsx) con su plan de trabajo y se crea un proyecto nuevo con su EDT, duraciones y dependencias.
        El sistema reconoce las columnas por su nombre (Tarea, Duración, Predecesoras, EDT, Nivel…) y usted puede corregir
        la asignación antes de importar. Los cálculos de fechas y costos los hace el sistema al cargar.
      </p>

      <div className="tarjeta">
        <h3>1. Archivo</h3>
        <input type="file" accept=".xlsx" onChange={alElegirArchivo} disabled={leyendo || importando} aria-label="Archivo Excel" />
        {leyendo && <p className="vacio" role="status">Leyendo el archivo…</p>}
        {nombreArchivo && !leyendo && <p className="vacio">Archivo: {nombreArchivo}</p>}
        <details>
          <summary>¿Qué formato espera?</summary>
          <ul className="imp-lista">
            <li>Una fila por tarea y una fila de encabezados. Solo se lee .xlsx (si tiene .xls o .csv, guárdelo como .xlsx).</li>
            <li>La jerarquía (etapa, actividad, subactividad) se toma de una columna de <b>código EDT</b> (1, 1.1, 1.1.1), de una columna de <b>nivel</b> o de la <b>sangría</b> del nombre. Si no hay ninguna, todas las filas pasan a una etapa única.</li>
            <li>Predecesoras como «3», «1.1.1», «3FS+2» o «4SS-1» (también FC, CC y CF de MS Project en español). Se buscan por identificador o por código EDT.</li>
            <li>La duración puede venir en días o calcularse con inicio y fin (lunes a viernes). Sin duración se usa 1 día.</li>
            <li>El sistema admite 3 niveles; lo que esté más abajo se importa como subactividad.</li>
          </ul>
        </details>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      {mapeo && analisis && (
        <form className="formulario" onSubmit={importar}>
          <div className="tarjeta">
            <h3>2. Columnas</h3>
            <div className="rejilla">
              {hojas.length > 1 && (
                <label>Hoja
                  <select value={hojaIdx} onChange={(e) => prepararHoja(hojas, Number(e.target.value), nombreArchivo)}>
                    {hojas.map((h, i) => <option key={h.nombre} value={i}>{h.nombre}</option>)}
                  </select>
                </label>
              )}
              <label>Fila de encabezados
                <input type="number" min={1} value={encabezado + 1} onChange={(e) => cambiarEncabezado(e.target.value)} />
              </label>
              {CAMPOS.map((campo) => (
                <label key={campo}>{ETIQUETA_CAMPO[campo]}{campo === "nombre" ? " *" : ""}
                  <select value={mapeo[campo] === null ? "" : String(mapeo[campo])} onChange={(e) => cambiarCampo(campo, e.target.value)}>
                    <option value="">— no usar —</option>
                    {Array.from({ length: columnas }, (_, i) => (
                      <option key={i} value={i}>{letra(i)} · {texto(titulos[i]) || "(sin título)"}</option>
                    ))}
                  </select>
                </label>
              ))}
            </div>
          </div>

          <div className="tarjeta">
            <h3>3. Proyecto nuevo</h3>
            <div className="rejilla">
              <label>Código
                <input value={codigo} onChange={(e) => setCodigo(e.target.value)} required maxLength={30} />
              </label>
              <label>Nombre
                <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={160} />
              </label>
              <label>Fecha de inicio
                <input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
              </label>
              <label>Presupuesto
                <input type="number" min={0} step="any" value={presupuesto} onChange={(e) => setPresupuesto(e.target.value)} />
              </label>
            </div>
            {!inicio && <p className="aviso">Sin fecha de inicio no se calculan fechas ni se dibuja el Gantt. Puede agregarla después editando el proyecto.</p>}
          </div>

          <div className="tarjeta">
            <h3>4. Vista previa</h3>
            {analisis.errores.map((m) => <p key={m} className="error" role="alert">{m}</p>)}
            {analisis.avisos.map((m) => <p key={m} className="aviso">{m}</p>)}
            {analisis.nodos.length > 0 && (
              <>
                <div className="cifras">
                  <div className="tarjeta">Etapas<strong>{etapas}</strong></div>
                  <div className="tarjeta">Tareas finales<strong>{hojasFinales}</strong></div>
                  <div className="tarjeta">Dependencias<strong>{analisis.dependencias.length}</strong></div>
                  <div className="tarjeta">Avisos<strong>{avisosFila}</strong></div>
                </div>
                <div className="imp-previa">
                  <table className="tabla">
                    <thead>
                      <tr><th>Fila</th><th>Tipo</th><th>Nombre</th><th className="num">Días</th><th className="num">Avance</th><th>Predecesoras</th><th>Avisos</th></tr>
                    </thead>
                    <tbody>
                      {analisis.nodos.slice(0, MAX_PREVIA).map((n, i) => (
                        <tr key={i}>
                          <td>{n.fila || "—"}</td>
                          <td>{tipoDeNivel(n.nivel)}</td>
                          <td className={`imp-nombre imp-tipo-${n.nivel}`} style={{ paddingLeft: 8 + (n.nivel - 1) * 18 }}>{n.nombre}</td>
                          <td className="num">{n.hijos > 0 ? "—" : (n.duracion ?? 1)}</td>
                          <td className="num">{n.avance === null ? "—" : `${n.avance} %`}</td>
                          <td>{n.predecesoras.map((p) => `${p.ref}${p.tipo}${p.desfase ? (p.desfase > 0 ? "+" : "") + p.desfase : ""}`).join(", ")}</td>
                          <td className="imp-aviso-celda">{n.avisos.join(" ")}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {analisis.nodos.length > MAX_PREVIA && <p className="vacio">Se muestran las primeras {MAX_PREVIA} de {analisis.nodos.length} filas.</p>}
              </>
            )}
            <div className="acciones">
              <button className="boton primario" type="submit" disabled={!puedeImportar}>
                {importando ? "Importando…" : "Importar proyecto"}
              </button>
            </div>
            {importando && (
              <div role="status">
                <div className="imp-barra"><div style={{ width: `${Math.round((avance.hecho / Math.max(1, avance.total)) * 100)}%` }} /></div>
                <p className="vacio">{avance.texto}</p>
              </div>
            )}
          </div>
        </form>
      )}

      {resultado && (
        <div className="tarjeta" role="status">
          <h3>Importación terminada</h3>
          <p className={resultado.fallos.length === 0 ? "exito" : "aviso"}>
            Proyecto {resultado.codigo}: {resultado.etapas} etapas, {resultado.actividades} actividades,{" "}
            {resultado.subactividades} subactividades y {resultado.dependencias} dependencias.
            {resultado.fallos.length > 0 && ` ${resultado.fallos.length} elementos no se pudieron cargar.`}
          </p>
          {resultado.fallos.length > 0 && (
            <ul className="imp-lista">{resultado.fallos.map((f) => <li key={f}>{f}</li>)}</ul>
          )}
          <div className="acciones">
            <Link className="boton primario" to={`/gantt?id=${resultado.proyectoId}`}>Ver en el Gantt</Link>
            <Link className="boton" to={`/proyectos?id=${resultado.proyectoId}`}>Ver el proyecto y su EDT</Link>
            {!confirmaBorrado ? (
              <button className="boton peligro" type="button" onClick={() => setConfirmaBorrado(true)}>Deshacer importación</button>
            ) : (
              <>
                <button className="boton peligro" type="button" onClick={deshacer}>Sí, borrar el proyecto importado</button>
                <button className="boton" type="button" onClick={() => setConfirmaBorrado(false)}>Cancelar</button>
              </>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
