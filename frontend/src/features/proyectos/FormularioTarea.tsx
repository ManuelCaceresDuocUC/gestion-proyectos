import { useState } from "react";
import type { FormEvent } from "react";
import { edtApi } from "../../api/planificacion";
import type { NodoEdt } from "../../types/planificacion";
import { mensajeDe } from "./formato";

interface Props {
  proyectoId: number;
  /** Tarea que se edita; si no hay, se crea una nueva bajo "padre" (o una etapa si tampoco hay padre). */
  tarea?: NodoEdt;
  padre?: NodoEdt;
  alGuardar: () => void;
  alCancelar: () => void;
}

/** Alta y edición de etapa, actividad o subactividad (A-11). El tipo lo decide el backend según el padre. */
export default function FormularioTarea({ proyectoId, tarea, padre, alGuardar, alCancelar }: Props) {
  const [nombre, setNombre] = useState(tarea?.nombre ?? "");
  const [descripcion, setDescripcion] = useState(tarea?.descripcion ?? "");
  const [duracion, setDuracion] = useState(String(tarea?.duracionDias ?? 1));
  const [avance, setAvance] = useState(String(tarea?.avance ?? 0));
  const [responsable, setResponsable] = useState(tarea?.responsableId ? String(tarea.responsableId) : "");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  // Los nodos con hijos obtienen duración y avance de sus hijos: solo se editan en las hojas.
  const esHoja = !tarea || tarea.hijos.length === 0;
  const etapaNueva = !tarea && !padre;
  const etiqueta = tarea ? `Editar ${tarea.codigo}` : padre ? `Nueva subtarea de ${padre.codigo}` : "Nueva etapa";

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setGuardando(true);
    try {
      const base = {
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || null,
        responsableId: responsable ? Number(responsable) : null,
        ...(esHoja && !etapaNueva ? { duracionDias: Number(duracion), avance: Number(avance) } : {}),
      };
      if (tarea) await edtApi.actualizar(tarea.id, { ...base, parentId: tarea.parentId });
      else await edtApi.crear(proyectoId, { ...base, parentId: padre?.id ?? null });
      alGuardar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h4>{etiqueta}</h4>
      <div className="rejilla">
        <label>Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={200} />
        </label>
        <label>Responsable (id)
          <input type="number" min={1} value={responsable} onChange={(e) => setResponsable(e.target.value)} />
        </label>
        {esHoja && !etapaNueva && (
          <>
            <label>Duración (días hábiles, 0 = hito)
              <input type="number" min={0} value={duracion} onChange={(e) => setDuracion(e.target.value)} required />
            </label>
            <label>Avance (%)
              <input type="number" min={0} max={100} step="any" value={avance} onChange={(e) => setAvance(e.target.value)} required />
            </label>
          </>
        )}
      </div>
      <label>Descripción
        <textarea rows={2} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
      </label>
      {tarea && !esHoja && <p className="aviso">Duración, fechas y avance de este nodo se calculan desde sus subtareas.</p>}
      {error && <p className="error" role="alert">{error}</p>}
      <div className="acciones">
        <button type="submit" className="boton primario" disabled={guardando}>{guardando ? "Guardando…" : "Guardar"}</button>
        <button type="button" className="boton" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
