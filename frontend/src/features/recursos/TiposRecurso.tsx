import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { tiposRecursoApi } from "../../api/recursos";
import type { TipoRecurso } from "../../types/recursos";
import Confirmar from "../proyectos/Confirmar";
import { mensajeDe } from "../proyectos/formato";
import { puedeEditar } from "../proyectos/permisos";

/** Alta, edición y baja de los tipos de recurso (B-10). */
export default function TiposRecurso() {
  const [tipos, setTipos] = useState<TipoRecurso[]>([]);
  const [editando, setEditando] = useState<TipoRecurso | "nuevo" | null>(null);
  const [aEliminar, setAEliminar] = useState<TipoRecurso | null>(null);
  const [error, setError] = useState("");
  const editable = puedeEditar();

  const cargar = useCallback(async () => {
    try {
      setTipos(await tiposRecursoApi.listar());
      setError("");
    } catch (e) {
      setError(mensajeDe(e));
    }
  }, []);

  useEffect(() => { void cargar(); }, [cargar]);

  async function eliminar(tipo: TipoRecurso) {
    setAEliminar(null);
    try {
      await tiposRecursoApi.eliminar(tipo.id);
      await cargar();
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  return (
    <section>
      <div className="encabezado">
        <h3>Tipos de recurso</h3>
        {editable && editando === null && (
          <button className="boton primario" onClick={() => setEditando("nuevo")}>Nuevo tipo</button>
        )}
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {editando && (
        <FormularioTipo
          tipo={editando === "nuevo" ? undefined : editando}
          alGuardar={() => { setEditando(null); void cargar(); }}
          alCancelar={() => setEditando(null)}
        />
      )}
      {tipos.length === 0 ? (
        <p className="vacio">Aún no hay tipos de recurso.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr><th>Nombre</th><th>Unidad</th><th /></tr>
          </thead>
          <tbody>
            {tipos.map((t) => (
              <tr key={t.id}>
                <td>{t.nombre}</td>
                <td>{t.unidad}</td>
                <td>
                  {editable && (
                    <div className="acciones" style={{ marginTop: 0, justifyContent: "flex-end" }}>
                      <button className="boton chico" onClick={() => setEditando(t)}>Editar</button>
                      <button className="boton chico peligro" onClick={() => setAEliminar(t)}>Eliminar</button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {aEliminar && (
        <Confirmar titulo="Eliminar tipo" textoConfirmar="Eliminar" peligro
          alConfirmar={() => void eliminar(aEliminar)} alCancelar={() => setAEliminar(null)}>
          <p>¿Eliminar el tipo «{aEliminar.nombre}»? Si tiene recursos asociados, no se puede borrar.</p>
        </Confirmar>
      )}
    </section>
  );
}

function FormularioTipo({ tipo, alGuardar, alCancelar }: {
  tipo?: TipoRecurso;
  alGuardar: () => void;
  alCancelar: () => void;
}) {
  const [nombre, setNombre] = useState(tipo?.nombre ?? "");
  const [unidad, setUnidad] = useState(tipo?.unidad ?? "hora");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setGuardando(true);
    setError("");
    try {
      const datos = { nombre: nombre.trim(), unidad: unidad.trim() };
      if (tipo) await tiposRecursoApi.actualizar(tipo.id, datos);
      else await tiposRecursoApi.crear(datos);
      alGuardar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h4>{tipo ? `Editar ${tipo.nombre}` : "Nuevo tipo"}</h4>
      <div className="rejilla">
        <label>Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={60} />
        </label>
        <label>Unidad
          <input value={unidad} onChange={(e) => setUnidad(e.target.value)} required maxLength={20} />
        </label>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="acciones">
        <button type="submit" className="boton primario" disabled={guardando}>{guardando ? "Guardando…" : "Guardar"}</button>
        <button type="button" className="boton" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
