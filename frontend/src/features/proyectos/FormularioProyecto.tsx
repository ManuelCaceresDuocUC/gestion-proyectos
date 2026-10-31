import { useState } from "react";
import type { FormEvent } from "react";
import { proyectosApi } from "../../api/planificacion";
import type { EstadoProyecto, Proyecto } from "../../types/planificacion";
import { ESTADOS_EDITABLES, ETIQUETA_ESTADO, mensajeDe } from "./formato";

const DIAS: Array<[string, string]> = [
  ["L", "Lun"], ["M", "Mar"], ["X", "Mié"], ["J", "Jue"], ["V", "Vie"], ["S", "Sáb"], ["D", "Dom"],
];

interface Props {
  proyecto?: Proyecto;
  alGuardar: (proyecto: Proyecto) => void;
  alCancelar: () => void;
}

/** Crear o editar un proyecto (A-10). */
export default function FormularioProyecto({ proyecto, alGuardar, alCancelar }: Props) {
  const [codigo, setCodigo] = useState(proyecto?.codigo ?? "");
  const [nombre, setNombre] = useState(proyecto?.nombre ?? "");
  const [descripcion, setDescripcion] = useState(proyecto?.descripcion ?? "");
  const [estado, setEstado] = useState<EstadoProyecto>(proyecto?.estado ?? "PLANIFICADO");
  const [responsable, setResponsable] = useState(proyecto?.responsableId ? String(proyecto.responsableId) : "");
  const [presupuesto, setPresupuesto] = useState(proyecto ? String(proyecto.presupuesto) : "0");
  const [moneda, setMoneda] = useState(proyecto?.moneda ?? "CLP");
  const [dias, setDias] = useState(proyecto?.diasLaborales ?? "LMXJV");
  const [inicio, setInicio] = useState(proyecto?.fechaInicio ?? "");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  function alternarDia(letra: string) {
    const nuevo = DIAS.map(([l]) => l).filter((l) => (l === letra ? !dias.includes(l) : dias.includes(l)));
    setDias(nuevo.join(""));
  }

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    if (!dias) {
      setError("Elija al menos un día laboral.");
      return;
    }
    const datos = {
      codigo: codigo.trim(),
      nombre: nombre.trim(),
      descripcion: descripcion.trim() || null,
      estado,
      responsableId: responsable ? Number(responsable) : null,
      presupuesto: Number(presupuesto),
      moneda: moneda.trim().toUpperCase(),
      diasLaborales: dias,
      fechaInicio: inicio || null,
    };
    setGuardando(true);
    try {
      const guardado = proyecto ? await proyectosApi.actualizar(proyecto.id, datos) : await proyectosApi.crear(datos);
      alGuardar(guardado);
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h3>{proyecto ? `Editar ${proyecto.codigo}` : "Nuevo proyecto"}</h3>
      <div className="rejilla">
        <label>Código
          <input value={codigo} onChange={(e) => setCodigo(e.target.value)} required maxLength={30} />
        </label>
        <label>Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={160} />
        </label>
        <label>Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value as EstadoProyecto)}>
            {ESTADOS_EDITABLES.map((s) => <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>)}
          </select>
        </label>
        <label>Responsable (id)
          <input type="number" min={1} value={responsable} onChange={(e) => setResponsable(e.target.value)} />
        </label>
        <label>Presupuesto
          <input type="number" min={0} step="any" value={presupuesto} onChange={(e) => setPresupuesto(e.target.value)} required />
        </label>
        <label>Moneda
          <input value={moneda} onChange={(e) => setMoneda(e.target.value)} minLength={3} maxLength={3} required />
        </label>
        <label>Fecha de inicio
          <input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
        </label>
      </div>
      <label>Descripción
        <textarea rows={2} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
      </label>
      <fieldset>
        <legend>Días laborales</legend>
        {DIAS.map(([letra, nombreDia]) => (
          <label key={letra} className="en-linea">
            <input type="checkbox" checked={dias.includes(letra)} onChange={() => alternarDia(letra)} /> {nombreDia}
          </label>
        ))}
      </fieldset>
      {!inicio && <p className="aviso">Sin fecha de inicio no se calculan las fechas de las tareas.</p>}
      {error && <p className="error" role="alert">{error}</p>}
      <div className="acciones">
        <button type="submit" className="boton primario" disabled={guardando}>{guardando ? "Guardando…" : "Guardar"}</button>
        <button type="button" className="boton" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
