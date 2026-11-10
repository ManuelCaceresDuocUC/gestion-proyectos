import { useEffect, useState } from "react";
import { edtApi, proyectosApi } from "../../api/planificacion";
import { recursosApi } from "../../api/recursos";
import { tablerosApi } from "../../api/tableros";
import type { NodoEdt, Proyecto } from "../../types/planificacion";
import type { Recurso } from "../../types/recursos";
import type { TableroProyecto as DatosProyecto } from "../../types/tablero";
import type { TableroCambio as DatosCambio } from "../../types/tableros";
import {
  ESTADOS_SOLICITUD,
  ETIQUETA_ESTADO,
  ETIQUETA_PRIORIDAD,
  ETIQUETA_RIESGO,
  PRIORIDADES,
  RIESGOS,
} from "../cambios/formato";
import { mensajeDe } from "../proyectos/formato";
import TableroCambio from "./TableroCambio";
import TableroProyecto from "./TableroProyecto";
import { etapasDe } from "./formato";

/** /tableros: proyecto (B-21) y cambio (C-12). Filtros y refresco al volver (C-13). */
export default function TablerosPagina() {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [recursos, setRecursos] = useState<Recurso[]>([]);
  const [etapas, setEtapas] = useState<NodoEdt[]>([]);
  const [proyectoId, setProyectoId] = useState("");
  const [etapaId, setEtapaId] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [responsableProyecto, setResponsableProyecto] = useState("");
  const [estado, setEstado] = useState("");
  const [prioridad, setPrioridad] = useState("");
  const [riesgo, setRiesgo] = useState("");
  const [responsableCambio, setResponsableCambio] = useState("");
  const [vista, setVista] = useState(0);
  const [tableroProyecto, setTableroProyecto] = useState<DatosProyecto | null>(null);
  const [tableroCambio, setTableroCambio] = useState<DatosCambio | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");

  const proyecto = proyectos.find((p) => String(p.id) === proyectoId);

  useEffect(() => {
    let vigente = true;
    Promise.all([proyectosApi.listar("", ""), recursosApi.listar(true)])
      .then(([lista, catalogo]) => {
        if (!vigente) return;
        setProyectos(lista);
        setRecursos(catalogo);
      })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    const alVer = () => {
      if (document.visibilityState === "visible") setVista((n) => n + 1);
    };
    document.addEventListener("visibilitychange", alVer);
    return () => document.removeEventListener("visibilitychange", alVer);
  }, []);

  useEffect(() => {
    setEtapaId("");
    if (!proyectoId) {
      setEtapas([]);
      return;
    }
    let vigente = true;
    edtApi.arbol(Number(proyectoId))
      .then((arbol) => { if (vigente) setEtapas(etapasDe(arbol)); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, [proyectoId]);

  useEffect(() => {
    if (!proyectoId) {
      setTableroProyecto(null);
      setTableroCambio(null);
      return;
    }
    const id = Number(proyectoId);
    let vigente = true;
    setCargando(true);
    Promise.all([
      tablerosApi.proyecto(id, {
        etapaId,
        desde,
        hasta,
        responsableId: responsableProyecto,
      }),
      tablerosApi.cambio(id, {
        estado,
        prioridad,
        riesgo,
        responsableId: responsableCambio,
      }),
    ])
      .then(([proyecto, cambio]) => {
        if (!vigente) return;
        setTableroProyecto(proyecto);
        setTableroCambio(cambio);
        setError("");
      })
      .catch((e) => {
        if (!vigente) return;
        setTableroProyecto(null);
        setTableroCambio(null);
        setError(mensajeDe(e));
      })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [
    proyectoId, etapaId, desde, hasta, responsableProyecto,
    estado, prioridad, riesgo, responsableCambio, vista,
  ]);

  return (
    <section>
      <h2>Tableros de comando</h2>
      <div className="filtros">
        <label>Proyecto
          <select value={proyectoId} onChange={(e) => setProyectoId(e.target.value)}>
            <option value="">Elija un proyecto</option>
            {proyectos.map((p) => (
              <option key={p.id} value={p.id}>{p.codigo} — {p.nombre}</option>
            ))}
          </select>
        </label>
        <label>Etapa
          <select value={etapaId} onChange={(e) => setEtapaId(e.target.value)} disabled={!proyectoId}>
            <option value="">Todas</option>
            {etapas.map((e) => (
              <option key={e.id} value={e.id}>{e.codigo} — {e.nombre}</option>
            ))}
          </select>
        </label>
        <label>Desde
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} disabled={!proyectoId} />
        </label>
        <label>Hasta
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} disabled={!proyectoId} />
        </label>
        <label>Responsable (proyecto)
          <select value={responsableProyecto} onChange={(e) => setResponsableProyecto(e.target.value)} disabled={!proyectoId}>
            <option value="">Todos</option>
            {recursos.map((r) => (
              <option key={r.id} value={r.id}>{r.nombre}</option>
            ))}
          </select>
        </label>
        <label>Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value)} disabled={!proyectoId}>
            <option value="">Todos</option>
            {ESTADOS_SOLICITUD.map((s) => (
              <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>
            ))}
          </select>
        </label>
        <label>Prioridad
          <select value={prioridad} onChange={(e) => setPrioridad(e.target.value)} disabled={!proyectoId}>
            <option value="">Todas</option>
            {PRIORIDADES.map((s) => (
              <option key={s} value={s}>{ETIQUETA_PRIORIDAD[s]}</option>
            ))}
          </select>
        </label>
        <label>Riesgo
          <select value={riesgo} onChange={(e) => setRiesgo(e.target.value)} disabled={!proyectoId}>
            <option value="">Todos</option>
            {RIESGOS.map((s) => (
              <option key={s} value={s}>{ETIQUETA_RIESGO[s]}</option>
            ))}
          </select>
        </label>
        <label>Responsable (cambio)
          <select value={responsableCambio} onChange={(e) => setResponsableCambio(e.target.value)} disabled={!proyectoId}>
            <option value="">Todos</option>
            {recursos.map((r) => (
              <option key={r.id} value={r.id}>{r.nombre}</option>
            ))}
          </select>
        </label>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {!proyectoId && <p className="vacio">Elija un proyecto para ver los dos tableros.</p>}
      {cargando && !tableroProyecto && !tableroCambio && <p className="vacio">Cargando tableros…</p>}

      {proyectoId && (
        <TableroProyecto proyectoId={proyectoId} datos={tableroProyecto} cargando={cargando} />
      )}
      {tableroCambio && proyecto && <TableroCambio datos={tableroCambio} moneda={proyecto.moneda || "CLP"} />}
    </section>
  );
}
