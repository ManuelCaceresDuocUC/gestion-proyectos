import { useCallback, useEffect, useState } from "react";
import { edtApi, proyectosApi } from "../../api/planificacion";
import type { NodoEdt, Proyecto } from "../../types/planificacion";
import ArbolEdt from "./ArbolEdt";
import Confirmar from "./Confirmar";
import FormularioProyecto from "./FormularioProyecto";
import PanelDependencias from "./PanelDependencias";
import { ETIQUETA_ESTADO, dinero, fecha, mensajeDe, porcentaje } from "./formato";
import { puedeEditar } from "./permisos";

interface Props {
  id: number;
  volver: () => void;
}

/** Ficha del proyecto con su EDT y dependencias. Cerrar pide confirmación; las acciones de edición se ocultan por rol/estado. */
export default function ProyectoDetalle({ id, volver }: Props) {
  const [proyecto, setProyecto] = useState<Proyecto | null>(null);
  const [arbol, setArbol] = useState<NodoEdt[]>([]);
  const [error, setError] = useState("");
  const [editando, setEditando] = useState(false);
  const [cerrando, setCerrando] = useState(false);
  const [eliminando, setEliminando] = useState(false);

  const cargar = useCallback(async () => {
    try {
      const [p, a] = await Promise.all([proyectosApi.obtener(id), edtApi.arbol(id)]);
      setProyecto(p);
      setArbol(a);
      setError("");
    } catch (e) {
      setError(mensajeDe(e));
    }
  }, [id]);

  useEffect(() => { void cargar(); }, [cargar]);

  if (!proyecto) {
    return <section>{error ? <p className="error" role="alert">{error}</p> : <p>Cargando…</p>}<button className="boton" onClick={volver}>← Proyectos</button></section>;
  }

  const editable = puedeEditar() && proyecto.estado !== "TERMINADO";

  async function cerrar() {
    setCerrando(false);
    try {
      setProyecto(await proyectosApi.cerrar(id));
      setError("");
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  async function eliminar() {
    setEliminando(false);
    try {
      await proyectosApi.eliminar(id);
      volver();
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  return (
    <section>
      <button className="boton" onClick={volver}>← Proyectos</button>
      <div className="encabezado">
        <h2>{proyecto.codigo} · {proyecto.nombre} <span className={`etiqueta estado-${proyecto.estado}`}>{ETIQUETA_ESTADO[proyecto.estado]}</span></h2>
        {editable && !editando && (
          <div className="acciones">
            <button className="boton" onClick={() => setEditando(true)}>Editar</button>
            <button className="boton" onClick={() => setCerrando(true)}>Cerrar proyecto</button>
            <button className="boton peligro" onClick={() => setEliminando(true)}>Eliminar</button>
          </div>
        )}
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {proyecto.estado === "TERMINADO" && <p className="aviso">Proyecto terminado: solo lectura.</p>}

      {editando ? (
        <FormularioProyecto proyecto={proyecto} alCancelar={() => setEditando(false)}
          alGuardar={(p) => { setProyecto(p); setEditando(false); void cargar(); }} />
      ) : (
        <dl className="tarjeta datos">
          <div><dt>Inicio</dt><dd>{fecha(proyecto.fechaInicio)}</dd></div>
          <div><dt>Fin planificado</dt><dd>{fecha(proyecto.fechaFinPlan)}</dd></div>
          <div><dt>Fin línea base</dt><dd>{fecha(proyecto.fechaFinBase)}</dd></div>
          <div><dt>Avance</dt><dd>{porcentaje(proyecto.avance)}</dd></div>
          <div><dt>Presupuesto</dt><dd>{dinero(proyecto.presupuesto, proyecto.moneda)}</dd></div>
          <div><dt>Costo planificado</dt><dd>{dinero(proyecto.costoPlan, proyecto.moneda)}</dd></div>
          <div><dt>Costo real</dt><dd>{dinero(proyecto.costoReal, proyecto.moneda)}</dd></div>
          <div><dt>Días laborales</dt><dd>{proyecto.diasLaborales}</dd></div>
          {proyecto.descripcion && <div><dt>Descripción</dt><dd>{proyecto.descripcion}</dd></div>}
        </dl>
      )}

      <ArbolEdt proyectoId={id} nodos={arbol} editable={editable} alCambiar={() => void cargar()} />
      <PanelDependencias proyectoId={id} arbol={arbol} editable={editable} alCambiar={() => void cargar()} />

      {cerrando && (
        <Confirmar titulo="Cerrar proyecto" textoConfirmar="Cerrar proyecto" alConfirmar={() => void cerrar()} alCancelar={() => setCerrando(false)}>
          <p>Al cerrar «{proyecto.nombre}» quedará como Terminado y no se podrá modificar. ¿Continuar?</p>
        </Confirmar>
      )}
      {eliminando && (
        <Confirmar titulo="Eliminar proyecto" textoConfirmar="Eliminar" peligro alConfirmar={() => void eliminar()} alCancelar={() => setEliminando(false)}>
          <p>Se eliminará «{proyecto.nombre}» con toda su EDT. Esta acción no se puede deshacer.</p>
        </Confirmar>
      )}
    </section>
  );
}
