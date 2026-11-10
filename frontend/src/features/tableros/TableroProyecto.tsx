import { useEffect, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { tablerosApi } from "../../api/tableros";
import { proyectosApi } from "../../api/planificacion";
import type { TableroProyecto } from "../../types/tablero";
import type { Proyecto } from "../../types/planificacion";
import { fecha, mensajeDe, dinero } from "../proyectos/formato";
import ListaSobreasignaciones from "../recursos/ListaSobreasignaciones";

const COLOR: Record<string, string> = {
  VERDE: "#1b7f4a",
  AMARILLO: "#a68400",
  ROJO: "#b42318",
};

function puntos(valor: number): string {
  return new Intl.NumberFormat("es-CL", { maximumFractionDigits: 2 }).format(valor);
}

function Grafico({ titulo, datos, color }: {
  titulo: string;
  datos: { nombre: string; valor: number }[];
  color: string;
}) {
  return (
    <article className="tarjeta">
      <h3>{titulo}</h3>
      <div style={{ width: "100%", height: 220 }}>
        <ResponsiveContainer>
          <BarChart data={datos}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="nombre" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="valor" fill={color} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </article>
  );
}

/** Tablero de proyecto (B-21). En `/tableros` la página manda `datos` (C-13). */
export default function TableroProyecto({
  proyectoId: proyectoFijo,
  datos,
  cargando: cargandoExterno = false,
}: {
  proyectoId?: string;
  datos?: TableroProyecto | null;
  cargando?: boolean;
}) {
  const controlado = datos !== undefined;
  const embebido = proyectoFijo !== undefined;
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [proyectoPropio, setProyectoPropio] = useState("");
  const proyectoId = embebido ? proyectoFijo : proyectoPropio;
  const [tableroInterno, setTableroInterno] = useState<TableroProyecto | null>(null);
  const [error, setError] = useState("");
  const [cargandoInterno, setCargandoInterno] = useState(false);
  const tablero = controlado ? datos ?? null : tableroInterno;
  const cargando = controlado ? cargandoExterno : cargandoInterno;

  useEffect(() => {
    if (embebido) return;
    let vigente = true;
    proyectosApi.listar("", "")
      .then((lista) => { if (vigente) setProyectos(lista); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, [embebido]);

  useEffect(() => {
    if (controlado) return;
    if (!proyectoId) {
      setTableroInterno(null);
      return;
    }
    let vigente = true;
    setCargandoInterno(true);
    tablerosApi.proyecto(Number(proyectoId))
      .then((respuesta) => {
        if (!vigente) return;
        setTableroInterno(respuesta);
        setError("");
      })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); })
      .finally(() => { if (vigente) setCargandoInterno(false); });
    return () => { vigente = false; };
  }, [proyectoId, controlado]);

  return (
    <section>
      <h3>Tablero de proyecto</h3>
      {!embebido && (
        <div className="filtros">
          <label>Proyecto
            <select value={proyectoId} onChange={(e) => setProyectoPropio(e.target.value)}>
              <option value="">Elija un proyecto</option>
              {proyectos.map((p) => (
                <option key={p.id} value={p.id}>{p.codigo} — {p.nombre}</option>
              ))}
            </select>
          </label>
        </div>
      )}
      {error && <p className="error" role="alert">{error}</p>}
      {!proyectoId && !embebido && <p className="vacio">Elija un proyecto para ver su tablero.</p>}
      {proyectoId && cargando && !controlado && <p className="vacio">Cargando tablero…</p>}
      {tablero && (
        <>
          <Grafico
            titulo="Avance real vs planificado"
            color="#2f5d8c"
            datos={[
              { nombre: "Planificado", valor: tablero.avance.planificado },
              { nombre: "Real", valor: tablero.avance.real },
            ]}
          />
          <Grafico
            titulo="Hitos"
            color="#3d6b4f"
            datos={[
              { nombre: "Cumplidos", valor: tablero.hitos.cumplidos.length },
              { nombre: "Próximos", valor: tablero.hitos.proximos.length },
              { nombre: "Vencidos", valor: tablero.hitos.vencidos.length },
            ]}
          />
          <Grafico
            titulo={`Costos (${tablero.costos.moneda})`}
            color="#8a5a2a"
            datos={[
              { nombre: "Presupuesto", valor: tablero.costos.presupuesto },
              { nombre: "Plan", valor: tablero.costos.plan },
              { nombre: "Real", valor: tablero.costos.real },
            ]}
          />
          <Grafico
            titulo="Profesionales"
            color="#6b3f6b"
            datos={[
              { nombre: "Asignados", valor: tablero.recursos.profesionales },
              { nombre: "Sobreasignados", valor: tablero.recursos.sobreasignados },
            ]}
          />

          <article className="tarjeta">
            <h3>KPI de proyecto e hitos</h3>
            {tablero.kpi.length === 0 ? (
              <p className="vacio">Todavía no hay un snapshot. Se escribe al recalcular el proyecto.</p>
            ) : (
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Nombre</th>
                    <th>Categoría</th>
                    <th className="num">Valor</th>
                    <th>Semáforo</th>
                  </tr>
                </thead>
                <tbody>
                  {tablero.kpi.map((item) => (
                    <tr key={item.id}>
                      <td>{item.codigo}</td>
                      <td>{item.nombre}</td>
                      <td>{item.categoria}</td>
                      <td className="num">{item.noCalculable || item.valor == null ? "No calculable" : puntos(item.valor)}</td>
                      <td>
                        <span
                          className="etiqueta"
                          style={{ background: item.semaforo ? COLOR[item.semaforo] : undefined, color: item.semaforo ? "#fff" : undefined }}
                        >
                          {item.noCalculable || !item.semaforo ? "No calculable" : item.semaforo}
                          {item.alerta ? " · alerta" : ""}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </article>

          <article className="tarjeta">
            <h3>Actividades atrasadas</h3>
            {tablero.actividadesAtrasadas.length === 0 ? (
              <p className="vacio">No hay hojas con fin planificado vencido y avance menor a 100 %.</p>
            ) : (
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Nombre</th>
                    <th>Fin plan</th>
                    <th className="num">Avance</th>
                  </tr>
                </thead>
                <tbody>
                  {tablero.actividadesAtrasadas.map((a) => (
                    <tr key={a.id}>
                      <td>{a.codigo}</td>
                      <td>{a.nombre}</td>
                      <td>{fecha(a.finPlan)}</td>
                      <td className="num">{puntos(a.avance)} %</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </article>

          <article className="tarjeta">
            <h3>Alertas de sobreasignación</h3>
            <p className="vacio">
              {tablero.recursos.sobreasignados} de {tablero.recursos.profesionales} profesionales.
              Plan {dinero(tablero.costos.plan, tablero.costos.moneda)}, real {dinero(tablero.costos.real, tablero.costos.moneda)}, presupuesto {dinero(tablero.costos.presupuesto, tablero.costos.moneda)}.
            </p>
            <ListaSobreasignaciones alertas={tablero.recursos.alertas} />
          </article>
        </>
      )}
    </section>
  );
}
