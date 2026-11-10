import { useEffect, useState } from "react";
import { proyectosApi } from "../../api/planificacion";
import { tablerosApi } from "../../api/tableros";
import type { Proyecto } from "../../types/planificacion";
import type { TableroCambio as DatosCambio } from "../../types/tableros";
import { mensajeDe } from "../proyectos/formato";
import TableroCambio from "./TableroCambio";

/** /tableros: comando de cambio (C-12). El de proyecto es B-21. */
export default function TablerosPagina() {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [proyectoId, setProyectoId] = useState("");
  const [tablero, setTablero] = useState<DatosCambio | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");

  const proyecto = proyectos.find((p) => String(p.id) === proyectoId);

  useEffect(() => {
    let vigente = true;
    proyectosApi.listar("", "")
      .then((lista) => { if (vigente) setProyectos(lista); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!proyectoId) {
      setTablero(null);
      return;
    }
    let vigente = true;
    setCargando(true);
    tablerosApi.cambio(Number(proyectoId))
      .then((datos) => { if (vigente) { setTablero(datos); setError(""); } })
      .catch((e) => { if (vigente) { setTablero(null); setError(mensajeDe(e)); } })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [proyectoId]);

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
      </div>
      {error && <p className="error" role="alert">{error}</p>}

      <div className="tarjeta">
        <h3>Proyecto</h3>
        <p className="vacio">Pendiente de implementar (B-21).</p>
      </div>

      {!proyectoId && <p className="vacio">Elija un proyecto para ver el tablero de gestión del cambio.</p>}
      {cargando && <p className="vacio">Cargando tablero de cambio…</p>}
      {tablero && proyecto && <TableroCambio datos={tablero} moneda={proyecto.moneda || "CLP"} />}
    </section>
  );
}
