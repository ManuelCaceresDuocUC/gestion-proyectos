import { Fragment, useState } from "react";
import { edtApi } from "../../api/planificacion";
import type { TipoTarea } from "../../types";
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
interface Plano { nodo: NodoEdt; profundidad: number }

const ETIQUETA_TIPO: Record<TipoTarea, string> = { ETAPA: "Etapa", ACTIVIDAD: "Actividad", SUBACTIVIDAD: "Subactividad" };

function aplanar(nodos: NodoEdt[], profundidad = 0): Plano[] {
  return nodos.flatMap((n) => [{ nodo: n, profundidad }, ...aplanar(n.hijos, profundidad + 1)]);
}

function buscar(nodos: NodoEdt[], id: number): NodoEdt | undefined {
  for (const n of nodos) {
    if (n.id === id) return n;
    const dentro = buscar(n.hijos, id);
    if (dentro) return dentro;
  }
  return undefined;
}

/** Lista de la EDT: etapa > actividad > subactividad, con filas separadas y alta, edición, borrado y reordenamiento (A-11). */
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

  const filas = aplanar(nodos);
  const nuevaEtapa = formulario !== null && !formulario.tarea && !formulario.padre;

  return (
    <div>
      {editable && (
        <div className="acciones" style={{ marginTop: 0, marginBottom: 8 }}>
          <button className="boton primario" onClick={() => setFormulario({})}>Nueva etapa</button>
        </div>
      )}
      {error && <p className="error" role="alert">{error}</p>}
      {nuevaEtapa && (
        <FormularioTarea proyectoId={proyectoId}
          alGuardar={() => { setFormulario(null); alCambiar(); }} alCancelar={() => setFormulario(null)} />
      )}

      <div className="edt-lista">
        <div className="edt-cab">
          <span>Tarea</span><span>Días</span><span>Plan (inicio → fin)</span><span>Avance</span><span />
        </div>
        {filas.length === 0 && <div className="edt-vacio">El proyecto aún no tiene etapas.</div>}
        {filas.map(({ nodo, profundidad }, i) => {
          const hermanos = hermanosDe(nodo);
          const posicion = hermanos.findIndex((h) => h.id === nodo.id) + 1;
          const formularioAqui = formulario?.tarea?.id === nodo.id || formulario?.padre?.id === nodo.id;
          return (
            <Fragment key={nodo.id}>
              <div className={`edt-fila edt-n${profundidad === 0 ? 0 : 1}${i % 2 === 1 ? " edt-par" : ""}`}>
                <div className="edt-nombre">
                  <span className="edt-sangria" style={{ width: profundidad * 22 }} />
                  <span className={`edt-tipo edt-tipo-${nodo.tipo}`}>{ETIQUETA_TIPO[nodo.tipo]}</span>
                  <span className="edt-codigo">{nodo.codigo}</span>
                  <span className="edt-texto" title={nodo.nombre}>{nodo.nombre}</span>
                </div>
                <div className="edt-dur">{nodo.duracionDias ?? "—"}</div>
                <div className="edt-fechas">{fecha(nodo.inicioPlan)} → {fecha(nodo.finPlan)}</div>
                <div className="edt-prog">
                  <div className="progreso"><div style={{ width: `${Math.min(100, nodo.avance)}%` }} /></div>
                  {porcentaje(nodo.avance)}
                </div>
                <div className="edt-acc">
                  {editable && (
                    <>
                      <button className="boton chico" title="Subir" disabled={posicion <= 1}
                        onClick={() => void ejecutar(() => edtApi.mover(nodo.id, posicion - 1))}>↑</button>
                      <button className="boton chico" title="Bajar" disabled={posicion >= hermanos.length}
                        onClick={() => void ejecutar(() => edtApi.mover(nodo.id, posicion + 1))}>↓</button>
                      {nodo.tipo !== "SUBACTIVIDAD" && (
                        <button className="boton chico" onClick={() => setFormulario({ padre: nodo })}>+ Agregar</button>
                      )}
                      <button className="boton chico" onClick={() => setFormulario({ tarea: nodo })}>Editar</button>
                      <button className="boton chico peligro" onClick={() => setAEliminar(nodo)}>Eliminar</button>
                    </>
                  )}
                </div>
              </div>
              {formularioAqui && (
                <div className="edt-form">
                  <FormularioTarea
                    key={`${nodo.id}-${formulario?.tarea ? "e" : "n"}`}
                    proyectoId={proyectoId}
                    tarea={formulario?.tarea}
                    padre={formulario?.padre}
                    alGuardar={() => { setFormulario(null); alCambiar(); }}
                    alCancelar={() => setFormulario(null)}
                  />
                </div>
              )}
            </Fragment>
          );
        })}
      </div>

      {aEliminar && (
        <Confirmar titulo={`Eliminar ${aEliminar.codigo}`} textoConfirmar="Eliminar" peligro
          alCancelar={() => setAEliminar(null)}
          alConfirmar={() => { const n = aEliminar; setAEliminar(null); void ejecutar(() => edtApi.eliminar(n.id)); }}>
          <p>¿Eliminar «{aEliminar.nombre}»?{aEliminar.hijos.length > 0 ? " También se eliminarán todas sus subtareas." : ""} Los códigos de las tareas siguientes se renumeran.</p>
        </Confirmar>
      )}
    </div>
  );
}
