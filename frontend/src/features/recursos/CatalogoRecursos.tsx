import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { recursosApi, tiposRecursoApi } from "../../api/recursos";
import type { Recurso, TipoRecurso } from "../../types/recursos";
import Confirmar from "../proyectos/Confirmar";
import { dinero, mensajeDe } from "../proyectos/formato";
import { puedeEditar } from "../proyectos/permisos";
import { esProfesional, numero } from "./formato";

/** Catálogo de recursos y profesionales: tarifa, capacidad diaria y estado (B-10). */
export default function CatalogoRecursos() {
  const [tipos, setTipos] = useState<TipoRecurso[]>([]);
  const [recursos, setRecursos] = useState<Recurso[]>([]);
  const [soloActivos, setSoloActivos] = useState(true);
  const [editando, setEditando] = useState<Recurso | "nuevo" | null>(null);
  const [aEliminar, setAEliminar] = useState<Recurso | null>(null);
  const [aviso, setAviso] = useState("");
  const [error, setError] = useState("");
  const editable = puedeEditar();

  const cargar = useCallback(async () => {
    try {
      const [t, r] = await Promise.all([tiposRecursoApi.listar(), recursosApi.listar(soloActivos)]);
      setTipos(t);
      setRecursos(r);
      setError("");
    } catch (e) {
      setError(mensajeDe(e));
    }
  }, [soloActivos]);

  useEffect(() => { void cargar(); }, [cargar]);

  const nombreTipo = (id: number) => tipos.find((t) => t.id === id)?.nombre ?? "—";

  async function eliminar(recurso: Recurso) {
    setAEliminar(null);
    try {
      await recursosApi.eliminar(recurso.id);
      setAviso("El recurso se eliminó. Si tenía asignaciones, quedó inactivo para conservar el historial.");
      await cargar();
    } catch (e) {
      setError(mensajeDe(e));
    }
  }

  return (
    <section>
      <div className="encabezado">
        <h3>Recursos y profesionales</h3>
        {editable && editando === null && (
          <button className="boton primario" onClick={() => setEditando("nuevo")} disabled={tipos.length === 0}>
            Nuevo recurso
          </button>
        )}
      </div>
      <label className="en-linea">
        <input type="checkbox" checked={soloActivos} onChange={(e) => setSoloActivos(e.target.checked)} />
        Solo activos
      </label>
      {tipos.length === 0 && <p className="aviso">Cree al menos un tipo de recurso antes de dar de alta profesionales o recursos.</p>}
      {error && <p className="error" role="alert">{error}</p>}
      {aviso && <p className="exito">{aviso}</p>}
      {editando && (
        <FormularioRecurso
          tipos={tipos}
          recurso={editando === "nuevo" ? undefined : editando}
          alGuardar={() => { setEditando(null); setAviso(""); void cargar(); }}
          alCancelar={() => setEditando(null)}
        />
      )}
      {recursos.length === 0 ? (
        <p className="vacio">No hay recursos{soloActivos ? " activos" : ""}.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Tipo</th>
              <th className="num">Tarifa</th>
              <th className="num">Capacidad / día</th>
              <th>Estado</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {recursos.map((r) => {
              const tipo = tipos.find((t) => t.id === r.tipoRecursoId);
              return (
                <tr key={r.id}>
                  <td>{r.nombre}</td>
                  <td>{nombreTipo(r.tipoRecursoId)}</td>
                  <td className="num">{dinero(r.tarifa, "CLP")}</td>
                  <td className="num">{r.capacidadHorasDia == null ? "—" : `${r.capacidadHorasDia} h`}</td>
                  <td>{r.activo ? "Activo" : "Inactivo"}{esProfesional(tipo) ? " · profesional" : ""}</td>
                  <td>
                    {editable && (
                      <div className="acciones" style={{ marginTop: 0, justifyContent: "flex-end" }}>
                        <button className="boton chico" onClick={() => setEditando(r)}>Editar</button>
                        <button className="boton chico peligro" onClick={() => setAEliminar(r)}>Eliminar</button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
      {aEliminar && (
        <Confirmar titulo="Eliminar recurso" textoConfirmar="Eliminar" peligro
          alConfirmar={() => void eliminar(aEliminar)} alCancelar={() => setAEliminar(null)}>
          <p>¿Eliminar «{aEliminar.nombre}»? Si ya está asignado a alguna tarea, solo se desactiva.</p>
        </Confirmar>
      )}
    </section>
  );
}

function FormularioRecurso({ tipos, recurso, alGuardar, alCancelar }: {
  tipos: TipoRecurso[];
  recurso?: Recurso;
  alGuardar: () => void;
  alCancelar: () => void;
}) {
  const [tipoId, setTipoId] = useState(String(recurso?.tipoRecursoId ?? tipos[0]?.id ?? ""));
  const [nombre, setNombre] = useState(recurso?.nombre ?? "");
  const [tarifa, setTarifa] = useState(recurso ? String(recurso.tarifa) : "");
  const [capacidad, setCapacidad] = useState(recurso?.capacidadHorasDia == null ? "" : String(recurso.capacidadHorasDia));
  const [activo, setActivo] = useState(recurso?.activo ?? true);
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  const tipo = tipos.find((t) => t.id === Number(tipoId));
  const profesional = esProfesional(tipo);

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    if (profesional && !capacidad.trim()) {
      setError("Un profesional debe declarar su capacidad diaria.");
      return;
    }
    setGuardando(true);
    try {
      const datos = {
        tipoRecursoId: Number(tipoId),
        nombre: nombre.trim(),
        tarifa: numero(tarifa),
        capacidadHorasDia: capacidad.trim() ? numero(capacidad) : null,
        activo,
      };
      if (recurso) await recursosApi.actualizar(recurso.id, datos);
      else await recursosApi.crear(datos);
      alGuardar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h4>{recurso ? `Editar ${recurso.nombre}` : "Nuevo recurso"}</h4>
      <div className="rejilla">
        <label>Tipo
          <select value={tipoId} onChange={(e) => setTipoId(e.target.value)} required>
            {tipos.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
          </select>
        </label>
        <label>Nombre
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} required maxLength={160} />
        </label>
        <label>Tarifa (CLP)
          <input type="number" min={0} step="any" value={tarifa} onChange={(e) => setTarifa(e.target.value)} required />
        </label>
        <label>Capacidad diaria (horas){profesional ? "" : " — opcional"}
          <input type="number" min={0} step="0.1" value={capacidad} onChange={(e) => setCapacidad(e.target.value)} required={profesional} />
        </label>
      </div>
      <label className="en-linea">
        <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} />
        Activo
      </label>
      {profesional && <p className="aviso">La capacidad diaria se usa para detectar sobreasignación.</p>}
      {error && <p className="error" role="alert">{error}</p>}
      <div className="acciones">
        <button type="submit" className="boton primario" disabled={guardando}>{guardando ? "Guardando…" : "Guardar"}</button>
        <button type="button" className="boton" onClick={alCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
