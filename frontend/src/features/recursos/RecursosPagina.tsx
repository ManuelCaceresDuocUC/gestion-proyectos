import { useEffect, useState } from "react";
import { sobreasignacionesApi } from "../../api/recursos";
import { proyectosApi } from "../../api/planificacion";
import type { AlertaSobreasignacion } from "../../types/recursos";
import type { Proyecto } from "../../types/planificacion";
import { mensajeDe } from "../proyectos/formato";
import CatalogoRecursos from "./CatalogoRecursos";
import ListaSobreasignaciones from "./ListaSobreasignaciones";
import TiposRecurso from "./TiposRecurso";

/** /recursos: administración de tipos, profesionales y alertas de sobreasignación (B-10). */
export default function RecursosPagina() {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [proyectoId, setProyectoId] = useState("");
  const [alertas, setAlertas] = useState<AlertaSobreasignacion[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let vigente = true;
    proyectosApi.listar("", "")
      .then((lista) => { if (vigente) setProyectos(lista); })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, []);

  useEffect(() => {
    if (!proyectoId) {
      setAlertas([]);
      return;
    }
    let vigente = true;
    sobreasignacionesApi.listar(Number(proyectoId))
      .then((lista) => { if (vigente) { setAlertas(lista); setError(""); } })
      .catch((e) => { if (vigente) setError(mensajeDe(e)); });
    return () => { vigente = false; };
  }, [proyectoId]);

  return (
    <section>
      <h2>Recursos, tiempos y costos</h2>
      <TiposRecurso />
      <CatalogoRecursos />
      <section>
        <h3>Sobreasignación por proyecto</h3>
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
        {proyectoId
          ? <ListaSobreasignaciones alertas={alertas} />
          : <p className="vacio">Elija un proyecto para ver los días en que un profesional supera su capacidad. La marca también aparece en la tarea, dentro del proyecto y del Gantt.</p>}
      </section>
    </section>
  );
}
