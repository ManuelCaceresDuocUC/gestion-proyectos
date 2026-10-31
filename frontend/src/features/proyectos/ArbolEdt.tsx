import { useState } from "react";
import { edtApi } from "../../api/planificacion";
import type { NodoEdt } from "../../types/planificacion";
import Confirmar from "./Confirmar";
import FormularioTarea from "./FormularioTarea";
import { fecha, mensajeDe, porcentaje } from "./formato";

interface Props {
  proyectoId: number;
  nodos: NodoEdt[];
  /** false = solo lectura (proyecto terminado o rol sin permiso). */
  editable: boolean;
  alCambiar: () => void;
}

type Formulario = { tarea?: NodoEdt; padre?: NodoEdt } | null;

/** Árbol de la EDT: etapa > actividad > subactividad, con alta, edición, borrado y reordenamiento (A-11). */
export default function ArbolEdt({ proyectoId, nodos, editable, alCambiar }: Props) {
  const [formulario, setFormulario] = useState<Formulario>(null);
  const [aEliminar, setAEliminar] = useState<NodoEdt | null>(null);
  const [error, setError] = useState("");

  async function ejecutar(accion: () => Promise<unknown>) {
    setError("");
    try {
      await accion();
      alCambiar();
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  function hermanosDe(nodo: NodoEdt): NodoEdt[] {
    return nodo.parentId === null ? nodos : buscar(nodos, nodo.parentId)?.hijos ?? [];
  }

  function renderNodo(nodo: NodoEdt) {
    const hermanos = hermanosDe(nodo);
    const posicion = hermanos.findIndex((h) => h.id === nodo.id) + 1;
    return (
      <li key={nodo.id} className={`tipo-${nodo.tipo}`}>
        <div className="nodo">
          <span className="codigo">{nodo.codigo}</span>
          <span>{nodo.nombre}</span>
          <span className="meta">
            {nodo.duracionDias !== null ? `${nodo.duracionDias} d · ` : ""}
            {fecha(nodo.inicioPlan)} → {fecha(nodo.finPlan)} · {porcentaje(nodo.avance)}
          </span>
          {editable && (
            <span className="acciones">
              <button className="boton chico" title="Subir" disabled={posicion <= 1}
                onClick={() => void ejecutar(() => edtApi.mover(nodo.id, posicion - 1))}>↑</button>
              <button className="boton chico" title="Bajar" disabled={posicion >= hermanos.length}
                onClick={() => void ejecutar(() => edtApi.mover(nodo.id, posicion + 1))}>↓</button>
              {nodo.tipo !== "SUBACTIVIDAD" && (
                <button className="boton chico" onClick={() => setFormulario({ padre: nodo })}>+ Agregar</button>
              )}
              <button className="boton chico" onClick={() => setFormulario({ tarea: nodo })}>Editar</button>
              <button className="boton chico peligro" onClick={() => setAEliminar(nodo)}>Eliminar</button>
            </span>
          )}
        </div>
        {formulario?.tarea?.id === nodo.id || formulario?.padre?.id === nodo.id ? (
          <FormularioTarea
            key={`${nodo.id}-${formulario?.tarea ? "e" : "n"}`}
            proyectoId={proyectoId}
            tarea={formulario?.tarea}
            padre={formulario?.padre}
            alGuardar={() => { setFormulario(null); alCambiar(); }}
            alCancelar={() => setFormulario(null)}
          />
        ) : null}
        {nodo.hijos.length > 0 && <ul>{nodo.hijos.map(renderNodo)}</ul>}
      </li>
    );
  }

  const hayHijos = aEliminar !== null && aEliminar.hijos.length > 0;
  const nuevaEtapa = formulario !== null && !formulario.tarea && !formulario.padre;

  return (
    <div className="edt">
      <div className="encabezado">
        <h3>Estructura de desglose del trabajo (EDT)</h3>
        {editable && <button className="boton primario" onClick={() => setFormulario({})}>Nueva etapa</button>}
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {nuevaEtapa && (
        <FormularioTarea proyectoId={proyectoId}
          alGuardar={() => { setFormulario(null); alCambiar(); }} alCancelar={() => setFormulario(null)} />
      )}
      {nodos.length === 0 ? <p className="vacio">El proyecto aún no tiene etapas.</p> : <ul>{nodos.map(renderNodo)}</ul>}
      {aEliminar && (
        <Confirmar titulo={`Eliminar ${aEliminar.codigo}`} textoConfirmar="Eliminar" peligro
          alCancelar={() => setAEliminar(null)}
          alConfirmar={() => { const n = aEliminar; setAEliminar(null); void ejecutar(() => edtApi.eliminar(n.id)); }}>
          <p>¿Eliminar «{aEliminar.nombre}»?{hayHijos ? " También se eliminarán todas sus subtareas." : ""} Los códigos de las tareas siguientes se renumeran.</p>
        </Confirmar>
      )}
    </div>
  );
}

function buscar(nodos: NodoEdt[], id: number): NodoEdt | undefined {
  for (const n of nodos) {
    if (n.id === id) return n;
    const dentro = buscar(n.hijos, id);
    if (dentro) return dentro;
  }
  return undefined;
}
