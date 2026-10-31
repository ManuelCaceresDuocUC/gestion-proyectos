import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ganttApi } from "../../api/gantt";
import { dependenciasApi, edtApi, proyectosApi } from "../../api/planificacion";
import type { ComparacionPlan } from "../../types/gantt";
import type { CambioFecha, DependenciaDto, NodoEdt, Proyecto } from "../../types/planificacion";
import { fecha, mensajeDe } from "../proyectos/formato";
import { puedeEditar } from "../proyectos/permisos";
import GanttGrafico, { ALTO_ENCABEZADO, ALTO_FILA } from "./GanttGrafico";
import PanelTarea from "./PanelTarea";
import { hoyIso } from "./escala";
import type { Escala } from "./escala";
import { aplanar } from "./modelo";
import { puedeEditarCronograma } from "./permisos";
import "./gantt.css";

/** /gantt?id=3: cronograma del proyecto 3 (A-15, A-16, A-17). Sin id, pide elegir un proyecto. */
export default function GanttVista() {
  const [params, setParams] = useSearchParams();
  const id = Number(params.get("id"));
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [arbol, setArbol] = useState<NodoEdt[]>([]);
  const [comparacion, setComparacion] = useState<ComparacionPlan | null>(null);
  const [dependencias, setDependencias] = useState<DependenciaDto[]>([]);
  const [escala, setEscala] = useState<Escala>("DIA");
  const [hoy, setHoy] = useState(hoyIso());
  const [plegados, setPlegados] = useState<Set<number>>(new Set());
  const [seleccionada, setSeleccionada] = useState<number | null>(null);
  const [cambios, setCambios] = useState<CambioFecha[]>([]);
  const [error, setError] = useState("");
  const valido = Number.isInteger(id) && id > 0;

  useEffect(() => {
    proyectosApi.listar("", "").then(setProyectos).catch((e) => setError(mensajeDe(e)));
  }, []);

  const cargar = useCallback(async () => {
    if (!valido) return;
    try {
      const [a, c, d] = await Promise.all([edtApi.arbol(id), ganttApi.comparacion(id, hoy), dependenciasApi.listar(id)]);
      setArbol(a);
      setComparacion(c);
      setDependencias(d);
      setError("");
    } catch (e) {
      setError(mensajeDe(e));
    }
  }, [id, hoy, valido]);

  useEffect(() => { void cargar(); }, [cargar]);

  const proyecto = proyectos.find((p) => p.id === id);
  const filas = useMemo(() => {
    const porTarea = new Map((comparacion?.tareas ?? []).map((t) => [t.tareaId, t]));
    return aplanar(arbol, porTarea, plegados);
  }, [arbol, comparacion, plegados]);
  const sinFechas = filas.length > 0 && filas.every((f) => !f.nodo.inicioPlan);
  const filaSel = filas.find((f) => f.nodo.id === seleccionada);
  const nombreDe = (n: number) => {
    const f = filas.find((x) => x.nodo.id === n);
    return f ? f.nodo.codigo : `#${n}`;
  };

  function alternar(nodoId: number) {
    setPlegados((prev) => {
      const nuevo = new Set(prev);
      if (nuevo.has(nodoId)) nuevo.delete(nodoId); else nuevo.add(nodoId);
      return nuevo;
    });
  }

  async function fijarBase() {
    try {
      await proyectosApi.fijarLineaBase(id, "");
      await cargar();
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  return (
    <section>
      <div className="encabezado">
        <h2>Gantt{proyecto ? ` · ${proyecto.codigo} ${proyecto.nombre}` : ""}</h2>
        <label className="en-linea">Proyecto{" "}
          <select value={valido ? String(id) : ""} onChange={(e) => { setSeleccionada(null); setCambios([]); setParams(e.target.value ? { id: e.target.value } : {}); }}>
            <option value="">Seleccione…</option>
            {proyectos.map((p) => <option key={p.id} value={p.id}>{p.codigo} · {p.nombre}</option>)}
          </select>
        </label>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {!valido && <p className="vacio">Elija un proyecto para ver su cronograma.</p>}

      {valido && (
        <>
          <div className="filtros">
            <label>Escala
              <select value={escala} onChange={(e) => setEscala(e.target.value as Escala)}>
                <option value="DIA">Días</option>
                <option value="SEMANA">Semanas</option>
              </select>
            </label>
            <label>Hoy (simular en demo)
              <input type="date" value={hoy} onChange={(e) => e.target.value && setHoy(e.target.value)} />
            </label>
            {puedeEditar() && proyecto?.estado !== "TERMINADO" && (
              <button className="boton" onClick={() => void fijarBase()}>Fijar línea base</button>
            )}
          </div>

          <p className="g-leyenda">
            <span className="g-l g-l-barra" /> Plan <span className="g-l g-l-avance" /> Avance <span className="g-l g-l-base" /> Línea base{" "}
            <span className="g-l g-l-atrasada" /> Atrasada <span className="g-l g-l-hito" /> Hito <span className="g-l g-l-hoy" /> Hoy
            {comparacion?.versionLineaBase == null ? " · Aún no hay línea base." : ` · Línea base v${comparacion.versionLineaBase}`}
            {comparacion?.resumen.desviacionFinDias != null && comparacion.resumen.desviacionFinDias !== 0 &&
              ` · Fin del proyecto ${comparacion.resumen.desviacionFinDias > 0 ? "+" : ""}${comparacion.resumen.desviacionFinDias} d vs base (${fecha(comparacion.resumen.finPlan)})`}
          </p>

          {sinFechas && <p className="aviso">El proyecto no tiene fechas calculadas: defina la fecha de inicio del proyecto.</p>}
          {filas.length === 0 && !error && <p className="vacio">El proyecto aún no tiene tareas.</p>}

          {filas.length > 0 && (
            <div className="g-contenedor">
              <div className="g-lista" style={{ paddingTop: ALTO_ENCABEZADO }}>
                {filas.map((f) => (
                  <div key={f.nodo.id} style={{ height: ALTO_FILA, paddingLeft: 6 + f.profundidad * 16 }}
                    className={`g-item${f.nodo.id === seleccionada ? " g-item-sel" : ""}${f.comp?.atrasada ? " g-item-atrasada" : ""}`}
                    onClick={() => setSeleccionada(f.nodo.id)}>
                    {f.esResumen ? (
                      <button className="g-plegar" aria-label={plegados.has(f.nodo.id) ? "Expandir" : "Plegar"}
                        onClick={(e) => { e.stopPropagation(); alternar(f.nodo.id); }}>{plegados.has(f.nodo.id) ? "▸" : "▾"}</button>
                    ) : <span className="g-plegar" />}
                    <span className="g-codigo">{f.nodo.codigo}</span> {f.nodo.nombre}
                  </div>
                ))}
              </div>
              <div className="g-area">
                <GanttGrafico filas={filas} dependencias={dependencias} escala={escala} hoy={hoy}
                  seleccionada={seleccionada} alSeleccionar={setSeleccionada} />
              </div>
            </div>
          )}

          {filaSel && (
            <PanelTarea fila={filaSel} dependencias={dependencias} nombreDe={nombreDe}
              editable={puedeEditarCronograma() && proyecto?.estado !== "TERMINADO"}
              alRecalcular={(c) => { setCambios(c); void cargar(); }} />
          )}

          {cambios.length > 0 && (
            <div className="exito">
              Recalculado — fechas que cambiaron:
              <ul>
                {cambios.map((c) => (
                  <li key={c.tareaId}>{c.codigo} {c.nombre}: {fecha(c.inicioAntes)}→{fecha(c.finAntes)} pasó a {fecha(c.inicioDespues)}→{fecha(c.finDespues)}</li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </section>
  );
}
