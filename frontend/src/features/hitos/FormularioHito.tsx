import { useState } from "react";
import type { FormEvent } from "react";
import { hitosApi } from "../../api/hitos";
import type { NodoEdt } from "../../types/planificacion";
import type { EstadoHito, Hito, HitoDatos } from "../../types/hitos";
import { fecha, mensajeDe } from "../proyectos/formato";
import Confirmar from "../proyectos/Confirmar";
import { ESTADOS_HITO, ETIQUETA_ESTADO_HITO } from "./formato";

interface Props {
  proyectoId: number;
  actividades: NodoEdt[];
  hito?: Hito;
  alGuardar: (guardado: Hito) => void;
  alCancelar: () => void;
  alEliminar?: () => void;
}

function iso(valor: string | null | undefined): string {
  return valor ? valor.slice(0, 10) : "";
}

function texto(valor: string): string | null {
  const limpio = valor.trim();
  return limpio ? limpio : null;
}

/** Alta o edición de la ficha. La semana se guarda como fecha prevista en el servidor. */
export default function FormularioHito({ proyectoId, actividades, hito, alGuardar, alCancelar, alEliminar }: Props) {
  const [codigo, setCodigo] = useState(hito?.codigo ?? "");
  const [logro, setLogro] = useState(hito?.logro ?? "");
  const [semana, setSemana] = useState(hito ? String(hito.semanaPrevista) : "");
  const [evidencia, setEvidencia] = useState(hito?.evidencia ?? "");
  const [indicador, setIndicador] = useState(hito?.indicador ?? "");
  const [criterio, setCriterio] = useState(hito?.criterioAceptacion ?? "");
  const [validador, setValidador] = useState(hito?.validador ?? "");
  const [accion, setAccion] = useState(hito?.accionSiNoCumple ?? "");
  const [estado, setEstado] = useState<EstadoHito>(hito?.estado ?? "PENDIENTE");
  const [fechaReal, setFechaReal] = useState(iso(hito?.fechaReal));
  const [tareas, setTareas] = useState<number[]>(hito?.tareaIds ?? []);
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [confirmarBorrado, setConfirmarBorrado] = useState(false);

  const cumplido = estado === "CUMPLIDO";

  function alternarTarea(id: number) {
    setTareas((actual) => actual.includes(id) ? actual.filter((x) => x !== id) : [...actual, id]);
  }

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    const semanaPrevista = Number(semana);
    if (!Number.isInteger(semanaPrevista) || semanaPrevista < 1) {
      setError("Indique la semana prevista, a partir de 1.");
      return;
    }
    if (tareas.length === 0) {
      setError("Asocie al menos una actividad.");
      return;
    }
    const datos: HitoDatos = {
      codigo: codigo.trim(),
      logro: logro.trim(),
      semanaPrevista,
      evidencia: texto(evidencia),
      indicador: texto(indicador),
      criterioAceptacion: texto(criterio),
      validador: texto(validador),
      accionSiNoCumple: texto(accion),
      estado,
      fechaReal: fechaReal || null,
      responsableId: hito?.responsableId ?? null,
      tareaIds: tareas,
    };
    setGuardando(true);
    try {
      const guardado = hito
        ? await hitosApi.actualizar(hito.id, datos)
        : await hitosApi.crear(proyectoId, datos);
      alGuardar(guardado);
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar() {
    if (!hito || !alEliminar) return;
    setError("");
    setGuardando(true);
    try {
      await hitosApi.eliminar(hito.id);
      alEliminar();
    } catch (err) {
      setError(mensajeDe(err));
      setGuardando(false);
      setConfirmarBorrado(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h3>{hito ? `Editar ${hito.codigo}` : "Nuevo hito"}</h3>
      <p className="vacio">
        Un hito es un logro de duración cero. La semana prevista se guarda como el último día hábil de esa semana
        {hito?.fechaPlan ? ` (hoy ${fecha(hito.fechaPlan)})` : ""}. Esa fecha tiene que ser igual o posterior al término de las actividades asociadas.
      </p>

      {error && <p className="error" role="alert">{error}</p>}

      <label>Código
        <input value={codigo} onChange={(e) => setCodigo(e.target.value)} maxLength={10} required />
      </label>
      <label>Logro verificable
        <input value={logro} onChange={(e) => setLogro(e.target.value)} maxLength={250} required />
      </label>
      <label>Semana prevista
        <input type="number" min={1} max={260} value={semana} onChange={(e) => setSemana(e.target.value)} required />
      </label>
      <label>Estado
        <select value={estado} onChange={(e) => setEstado(e.target.value as EstadoHito)}>
          {ESTADOS_HITO.map((s) => <option key={s} value={s}>{ETIQUETA_ESTADO_HITO[s]}</option>)}
        </select>
      </label>
      <label>Fecha real
        <input type="date" value={fechaReal} onChange={(e) => setFechaReal(e.target.value)} required={cumplido} />
      </label>
      <label>Evidencia y fuente de datos
        <textarea value={evidencia} onChange={(e) => setEvidencia(e.target.value)} rows={2} required={cumplido} />
      </label>
      <label>Indicador / forma de cálculo
        <textarea value={indicador} onChange={(e) => setIndicador(e.target.value)} rows={2} />
      </label>
      <label>Unidad y criterio de aceptación
        <textarea value={criterio} onChange={(e) => setCriterio(e.target.value)} rows={2} />
      </label>
      <label>Quién valida y cuándo
        <input value={validador} onChange={(e) => setValidador(e.target.value)} maxLength={160} />
      </label>
      <label>Acción si no se cumple
        <textarea value={accion} onChange={(e) => setAccion(e.target.value)} rows={2} />
      </label>

      <fieldset>
        <legend>Actividades asociadas</legend>
        {actividades.length === 0 && <p className="vacio">Este proyecto no tiene actividades.</p>}
        <div style={{ maxHeight: 220, overflow: "auto" }}>
          {actividades.map((a) => (
            <label key={a.id} className="en-linea">
              <input
                type="checkbox"
                checked={tareas.includes(a.id)}
                onChange={() => alternarTarea(a.id)}
              />
              {a.codigo} {a.nombre}
              {a.finPlan ? ` · término ${fecha(a.finPlan)}` : ""}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="acciones">
        <button className="boton primario" type="submit" disabled={guardando}>
          {guardando ? "Guardando…" : "Guardar"}
        </button>
        <button className="boton" type="button" onClick={alCancelar} disabled={guardando}>Cancelar</button>
        {hito && alEliminar && (
          <button className="boton peligro" type="button" onClick={() => setConfirmarBorrado(true)} disabled={guardando}>
            Eliminar
          </button>
        )}
      </div>

      {confirmarBorrado && hito && (
        <Confirmar
          titulo={`Eliminar ${hito.codigo}`}
          textoConfirmar="Eliminar"
          peligro
          alConfirmar={eliminar}
          alCancelar={() => setConfirmarBorrado(false)}
        >
          <p>Se borra el hito y sus actividades asociadas.</p>
        </Confirmar>
      )}
    </form>
  );
}
