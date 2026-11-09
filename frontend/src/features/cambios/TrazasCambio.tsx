import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { cambiosApi } from "../../api/cambios";
import type { CambioHistorial, Evidencia } from "../../types/cambios";
import {
  enlaceHttp,
  etiquetaCampoHistorial,
  etiquetaValorHistorial,
  fechaHora,
  mensajeDe,
} from "./formato";

interface EvidenciasProps {
  solicitudId: number;
  editable: boolean;
}

/** Alta, listado y baja de evidencias (texto + URL; C-05). */
export function PanelEvidencias({ solicitudId, editable }: EvidenciasProps) {
  const [filas, setFilas] = useState<Evidencia[]>([]);
  const [descripcion, setDescripcion] = useState("");
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);

  const cargar = useCallback(async () => {
    try {
      setFilas(await cambiosApi.evidencias(solicitudId));
      setError("");
    } catch (err) {
      setError(mensajeDe(err));
    }
  }, [solicitudId]);

  useEffect(() => { void cargar(); }, [cargar]);

  async function alta(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const texto = descripcion.trim();
    if (!texto) return;
    setOcupado(true);
    try {
      await cambiosApi.crearEvidencia(solicitudId, {
        descripcion: texto,
        url: url.trim() || null,
      });
      setDescripcion("");
      setUrl("");
      await cargar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setOcupado(false);
    }
  }

  async function baja(evidencia: Evidencia) {
    if (!window.confirm(`¿Quitar la evidencia «${evidencia.descripcion}»?`)) return;
    setOcupado(true);
    try {
      await cambiosApi.eliminarEvidencia(solicitudId, evidencia.id);
      await cargar();
    } catch (err) {
      setError(mensajeDe(err));
    } finally {
      setOcupado(false);
    }
  }

  return (
    <>
      <h3>Evidencias</h3>
      {error && <p className="error" role="alert">{error}</p>}
      {editable && (
        <form className="formulario tarjeta" onSubmit={(ev) => void alta(ev)}>
          <div className="rejilla">
            <label>Descripción
              <input
                value={descripcion}
                onChange={(ev) => setDescripcion(ev.target.value)}
                required
                maxLength={250}
                disabled={ocupado}
              />
            </label>
            <label>URL (opcional)
              <input
                value={url}
                onChange={(ev) => setUrl(ev.target.value)}
                maxLength={500}
                disabled={ocupado}
              />
            </label>
          </div>
          <div className="acciones">
            <button type="submit" className="boton primario" disabled={ocupado}>Añadir</button>
          </div>
        </form>
      )}
      {filas.length === 0 ? (
        <p className="vacio">
          {editable
            ? "No hay evidencias. Basta una descripción; la URL es opcional y no se suben archivos."
            : "No hay evidencias."}
        </p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Descripción</th>
              <th>URL</th>
              <th>Fecha</th>
              {editable && <th />}
            </tr>
          </thead>
          <tbody>
            {filas.map((e) => {
              const href = enlaceHttp(e.url);
              return (
                <tr key={e.id}>
                  <td>{e.descripcion}</td>
                  <td>
                    {href ? (
                      <a href={href} target="_blank" rel="noopener noreferrer">{e.url}</a>
                    ) : (e.url ?? "—")}
                  </td>
                  <td>{fechaHora(e.creadoEn)}</td>
                  {editable && (
                    <td>
                      <button
                        type="button"
                        className="boton chico peligro"
                        disabled={ocupado}
                        onClick={() => void baja(e)}
                      >
                        Quitar
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </>
  );
}

interface HistorialProps {
  filas: CambioHistorial[];
}

/** Bitácora de solo lectura: estado de la solicitud y de cada etapa. */
export function ListaHistorial({ filas }: HistorialProps) {
  return (
    <>
      <h3>Historial</h3>
      {filas.length === 0 ? (
        <p className="vacio">Todavía no hay movimientos. Quedan registrados los cambios de estado de la solicitud y de las etapas.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Campo</th>
              <th>Antes</th>
              <th>Después</th>
              <th>Usuario</th>
            </tr>
          </thead>
          <tbody>
            {filas.map((h) => (
              <tr key={h.id}>
                <td>{fechaHora(h.fecha)}</td>
                <td>{etiquetaCampoHistorial(h.campo)}</td>
                <td>{etiquetaValorHistorial(h.campo, h.valorAnterior)}</td>
                <td>{etiquetaValorHistorial(h.campo, h.valorNuevo)}</td>
                <td>{h.usuarioId ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
