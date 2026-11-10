import { useEffect, useState } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { kpiApi } from "../../api/kpi";
import { proyectosApi } from "../../api/planificacion";
import type { KpiDefinicion, KpiEstado, KpiHistorico } from "../../types/kpi";
import type { Proyecto } from "../../types/planificacion";
import { mensajeDe } from "../proyectos/formato";
import { textoCumplimiento, textoSemaforo, textoValorKpi } from "../tableros/formato";
import FormularioKpi from "./FormularioKpi";
import { ETIQUETA_CATEGORIA, ETIQUETA_SENTIDO, datosDe } from "./formato";

function marca(iso: string): string {
  return new Date(iso).toLocaleString("es-CL", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/** Administración de definiciones (B-15). El semáforo y el histórico ya vienen calculados. */
export default function KpiPagina() {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [proyectoId, setProyectoId] = useState("");
  const [definiciones, setDefiniciones] = useState<KpiDefinicion[]>([]);
  const [estados, setEstados] = useState<KpiEstado[]>([]);
  const [seleccionId, setSeleccionId] = useState<number | null>(null);
  const [historico, setHistorico] = useState<KpiHistorico[]>([]);
  const [formulario, setFormulario] = useState<KpiDefinicion | "nuevo" | null>(null);
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const [tick, setTick] = useState(0);

  const proyecto = proyectos.find((p) => String(p.id) === proyectoId);
  const cerrado = proyecto?.estado === "TERMINADO";
  const seleccion = definiciones.find((d) => d.id === seleccionId) ?? null;
  const estado = estados.find((e) => e.id === seleccionId) ?? null;
  const puntos = [...historico].reverse().flatMap((fila) => (
    fila.valor == null ? [] : [{ fecha: marca(fila.calculadoEn), valor: fila.valor }]
  ));

  useEffect(() => {
    let vigente = true;
    proyectosApi.listar("", "")
      .then((lista) => { if (vigente) setProyectos(lista); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!proyectoId) {
      setDefiniciones([]);
      setEstados([]);
      setHistorico([]);
      return;
    }
    let vigente = true;
    setCargando(true);
    const id = Number(proyectoId);
    Promise.all([kpiApi.definiciones(id), kpiApi.estados(id)])
      .then(([defs, snaps]) => {
        if (!vigente) return;
        setDefiniciones(defs);
        setEstados(snaps);
        setError("");
      })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [proyectoId, tick]);

  useEffect(() => {
    if (!seleccionId || !proyectoId) {
      setHistorico([]);
      return;
    }
    let vigente = true;
    kpiApi.historico(seleccionId, Number(proyectoId))
      .then((serie) => { if (vigente) setHistorico(serie); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, [seleccionId, proyectoId, tick]);

  function puedeEscribir(definicion: KpiDefinicion): boolean {
    return definicion.proyectoId == null || !cerrado;
  }

  async function cambiarActivo(definicion: KpiDefinicion) {
    setError("");
    try {
      await kpiApi.actualizar(definicion.id, datosDe(definicion, !definicion.activo));
      setTick((n) => n + 1);
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  return (
    <section>
      <div className="encabezado">
        <h2>KPI</h2>
        {proyectoId && !formulario && (
          <button className="boton primario" onClick={() => setFormulario("nuevo")}>Nuevo KPI</button>
        )}
      </div>
      <p className="vacio">
        Definiciones del proyecto y las globales. Activar o desactivar no recalcula: el semáforo es el del último snapshot.
      </p>

      <div className="filtros">
        <label>Proyecto
          <select value={proyectoId} onChange={(e) => {
            setProyectoId(e.target.value);
            setCargando(Boolean(e.target.value));
            setFormulario(null);
            setSeleccionId(null);
          }}>
            <option value="">Elija un proyecto</option>
            {proyectos.map((p) => (
              <option key={p.id} value={p.id}>{p.codigo} — {p.nombre}</option>
            ))}
          </select>
        </label>
      </div>

      {cerrado && <p className="aviso">El proyecto está cerrado. Puede editar KPI globales, no los de este proyecto.</p>}
      {error && <p className="error" role="alert">{error}</p>}

      {formulario && proyectoId && (
        <FormularioKpi
          key={formulario === "nuevo" ? "nuevo" : formulario.id}
          proyectoId={Number(proyectoId)}
          cerrado={cerrado}
          definicion={formulario === "nuevo" ? undefined : formulario}
          alGuardar={() => { setFormulario(null); setTick((n) => n + 1); }}
          alCancelar={() => setFormulario(null)}
          alEliminar={formulario === "nuevo" ? undefined : () => {
            setSeleccionId(null);
            setFormulario(null);
            setTick((n) => n + 1);
          }}
        />
      )}

      {!proyectoId && <p className="vacio">Elija un proyecto para ver y administrar sus KPI.</p>}
      {proyectoId && cargando && <p className="vacio">Cargando KPI…</p>}
      {proyectoId && !cargando && definiciones.length === 0 && !formulario && (
        <p className="vacio">No hay definiciones. El catálogo se carga en el primer recálculo del proyecto.</p>
      )}

      {definiciones.length > 0 && (
        <table className="tabla">
          <thead>
            <tr>
              <th>Código</th>
              <th>Nombre</th>
              <th>Categoría</th>
              <th>Alcance</th>
              <th>Estado</th>
              <th>Semáforo</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {definiciones.map((definicion) => {
              const snap = estados.find((e) => e.id === definicion.id);
              const escribir = puedeEscribir(definicion);
              return (
                <tr key={definicion.id}>
                  <td>{definicion.codigo}</td>
                  <td>{definicion.nombre}</td>
                  <td>{ETIQUETA_CATEGORIA[definicion.categoria]}</td>
                  <td>{definicion.proyectoId == null ? "Global" : "Proyecto"}</td>
                  <td>{definicion.activo ? "Activo" : "Inactivo"}</td>
                  <td>
                    {!definicion.activo && "—"}
                    {definicion.activo && !snap?.calculadoEn && "Sin snapshot"}
                    {definicion.activo && snap?.calculadoEn && (
                      <span className={`etiqueta semaforo-${snap.noCalculable ? "NULO" : snap.semaforo ?? "NULO"}`}>
                        {textoSemaforo(snap.semaforo, snap.noCalculable)}
                      </span>
                    )}
                  </td>
                  <td>
                    <button className="boton chico" onClick={() => setSeleccionId(definicion.id)}>Ver</button>
                    {escribir && (
                      <>
                        <button className="boton chico" onClick={() => setFormulario(definicion)}>Editar</button>
                        <button className="boton chico" onClick={() => cambiarActivo(definicion)}>
                          {definicion.activo ? "Desactivar" : "Activar"}
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}

      {seleccion && (
        <article className="tarjeta">
          <h3>{seleccion.codigo} — {seleccion.nombre}</h3>
          <p>{seleccion.formula}</p>
          <p className="vacio">
            Variables: {seleccion.variables.length ? seleccion.variables.join(", ") : "ninguna"}.
            {" "}Sentido: {ETIQUETA_SENTIDO[seleccion.sentido]}.
            {seleccion.responsable ? ` Responsable: ${seleccion.responsable}.` : ""}
          </p>
          <p>
            Valor: {estado?.calculadoEn
              ? textoValorKpi(estado.valor, estado.unidad, estado.noCalculable)
              : "Sin snapshot"}
            {" · "}
            Cumplimiento: {estado?.calculadoEn ? textoCumplimiento(estado.cumplimiento) : "—"}
            {" · "}
            {estado?.calculadoEn
              ? textoSemaforo(estado.semaforo, estado.noCalculable)
              : "El semáforo aparece después de un recálculo."}
          </p>
          {puntos.length === 0 ? (
            <p className="vacio">No hay valores históricos para graficar en este proyecto.</p>
          ) : (
            <div style={{ width: "100%", height: 220 }}>
              <ResponsiveContainer>
                <LineChart data={puntos}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="fecha" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="valor" name="Valor" stroke="#1b4f72" dot />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>
      )}
    </section>
  );
}
