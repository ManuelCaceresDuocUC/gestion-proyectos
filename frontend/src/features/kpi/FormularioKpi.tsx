import { useState } from "react";
import type { FormEvent } from "react";
import { kpiApi } from "../../api/kpi";
import type { CategoriaKpi, KpiDefinicion, OperadorMeta, SentidoKpi } from "../../types/kpi";
import { mensajeDe } from "../proyectos/formato";
import Confirmar from "../proyectos/Confirmar";
import {
  CATEGORIAS,
  ETIQUETA_CATEGORIA,
  ETIQUETA_OPERADOR,
  ETIQUETA_SENTIDO,
  OPERADORES,
  SENTIDOS,
  variablesDe,
} from "./formato";

interface Props {
  proyectoId: number;
  cerrado: boolean;
  definicion?: KpiDefinicion;
  alGuardar: () => void;
  alCancelar: () => void;
  alEliminar?: () => void;
}

function texto(valor: string): string | null {
  const limpio = valor.trim();
  return limpio ? limpio : null;
}

function decimal(valor: string): number | null {
  const limpio = valor.trim().replace(",", ".");
  if (!limpio) return null;
  const numero = Number(limpio);
  return Number.isFinite(numero) ? numero : null;
}

/** Alta o edición de la definición. El semáforo lo escribe el recálculo, no este formulario. */
export default function FormularioKpi({
  proyectoId, cerrado, definicion, alGuardar, alCancelar, alEliminar,
}: Props) {
  const [global, setGlobal] = useState(definicion ? definicion.proyectoId == null : cerrado);
  const [codigo, setCodigo] = useState(definicion?.codigo ?? "");
  const [nombre, setNombre] = useState(definicion?.nombre ?? "");
  const [categoria, setCategoria] = useState<CategoriaKpi>(definicion?.categoria ?? "PROYECTO");
  const [formula, setFormula] = useState(definicion?.formula ?? "");
  const [unidad, setUnidad] = useState(definicion?.unidad ?? "%");
  const [sentido, setSentido] = useState<SentidoKpi>(definicion?.sentido ?? "MAYOR_MEJOR");
  const [operador, setOperador] = useState<OperadorMeta>(definicion?.operadorMeta ?? "MAYOR_IGUAL");
  const [meta, setMeta] = useState(definicion?.meta == null ? "" : String(definicion.meta));
  const [metaMax, setMetaMax] = useState(definicion?.metaMax == null ? "" : String(definicion.metaMax));
  const [umbralVerde, setUmbralVerde] = useState(definicion ? String(definicion.umbralVerde) : "1");
  const [umbralAmarillo, setUmbralAmarillo] = useState(definicion ? String(definicion.umbralAmarillo) : "0.8");
  const [responsable, setResponsable] = useState(definicion?.responsable ?? "");
  const [activo, setActivo] = useState(definicion?.activo ?? true);
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [confirmarBorrado, setConfirmarBorrado] = useState(false);

  const catalogo = variablesDe(categoria);

  function insertar(nombreVariable: string) {
    setFormula((actual) => (actual.trim() ? `${actual.trim()} ${nombreVariable}` : nombreVariable));
  }

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    const verde = decimal(umbralVerde);
    const amarillo = decimal(umbralAmarillo);
    if (verde == null || amarillo == null) {
      setError("Indique el umbral verde y el umbral amarillo.");
      return;
    }
    if (operador === "ENTRE" && (decimal(meta) == null || decimal(metaMax) == null)) {
      setError("El operador Entre requiere la meta y la meta máxima.");
      return;
    }
    const datos = {
      proyectoId: global ? null : proyectoId,
      codigo: codigo.trim(),
      nombre: nombre.trim(),
      categoria,
      formula: formula.trim(),
      unidad: texto(unidad),
      sentido,
      operadorMeta: operador,
      meta: decimal(meta),
      metaMax: operador === "ENTRE" ? decimal(metaMax) : null,
      umbralVerde: verde,
      umbralAmarillo: amarillo,
      responsable: texto(responsable),
      activo,
    };
    setGuardando(true);
    try {
      if (definicion) await kpiApi.actualizar(definicion.id, datos);
      else await kpiApi.crear(datos);
      alGuardar();
    } catch (err) {
      setError(mensajeDe(err));
      setGuardando(false);
    }
  }

  async function eliminar() {
    if (!definicion || !alEliminar) return;
    setError("");
    setGuardando(true);
    try {
      await kpiApi.eliminar(definicion.id);
      alEliminar();
    } catch (err) {
      setError(mensajeDe(err));
      setGuardando(false);
      setConfirmarBorrado(false);
    }
  }

  return (
    <form className="tarjeta formulario" onSubmit={enviar}>
      <h3>{definicion ? `Editar ${definicion.codigo}` : "Nuevo KPI"}</h3>
      <p className="vacio">
        La fórmula usa solo variables del catálogo de la categoría. Si la unidad es %, escriba una razón:
        el recálculo la pasa a puntos porcentuales. El valor y el semáforo se leen del último snapshot;
        esta pantalla no los calcula.
      </p>
      {definicion != null && definicion.proyectoId == null && (
        <p className="aviso">Esta definición es global: el cambio aplica a todos los proyectos.</p>
      )}
      {error && <p className="error" role="alert">{error}</p>}

      <label className="en-linea">
        <input
          type="checkbox"
          checked={global}
          disabled={cerrado}
          onChange={(e) => setGlobal(e.target.checked)}
        />
        Definición global
      </label>
      {cerrado && <p className="aviso">El proyecto está cerrado: solo puede guardar un KPI global.</p>}

      <label>Código
        <input value={codigo} onChange={(e) => setCodigo(e.target.value)} maxLength={20} required />
      </label>
      <label>Nombre
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={200} required />
      </label>
      <label>Categoría
        <select value={categoria} onChange={(e) => setCategoria(e.target.value as CategoriaKpi)}>
          {CATEGORIAS.map((c) => <option key={c} value={c}>{ETIQUETA_CATEGORIA[c]}</option>)}
        </select>
      </label>
      <label>Fórmula
        <textarea value={formula} onChange={(e) => setFormula(e.target.value)} rows={2} required />
      </label>
      <fieldset>
        <legend>Variables permitidas</legend>
        <div className="acciones">
          {catalogo.map((nombreVariable) => (
            <button
              key={nombreVariable}
              className="boton chico"
              type="button"
              onClick={() => insertar(nombreVariable)}
            >
              {nombreVariable}
            </button>
          ))}
        </div>
      </fieldset>
      <label>Unidad
        <input value={unidad} onChange={(e) => setUnidad(e.target.value)} maxLength={20} placeholder="%, días, índice" />
      </label>
      <label>Sentido
        <select value={sentido} onChange={(e) => setSentido(e.target.value as SentidoKpi)}>
          {SENTIDOS.map((s) => <option key={s} value={s}>{ETIQUETA_SENTIDO[s]}</option>)}
        </select>
      </label>
      <label>Operador de la meta
        <select value={operador} onChange={(e) => setOperador(e.target.value as OperadorMeta)}>
          {OPERADORES.map((o) => <option key={o} value={o}>{ETIQUETA_OPERADOR[o]}</option>)}
        </select>
      </label>
      <label>Meta
        <input value={meta} onChange={(e) => setMeta(e.target.value)} inputMode="decimal" required={operador === "ENTRE"} />
      </label>
      {operador === "ENTRE" && (
        <label>Meta máxima
          <input value={metaMax} onChange={(e) => setMetaMax(e.target.value)} inputMode="decimal" required />
        </label>
      )}
      <label>Umbral verde
        <input value={umbralVerde} onChange={(e) => setUmbralVerde(e.target.value)} inputMode="decimal" required />
      </label>
      <label>Umbral amarillo
        <input value={umbralAmarillo} onChange={(e) => setUmbralAmarillo(e.target.value)} inputMode="decimal" required />
      </label>
      <label>Responsable
        <input value={responsable} onChange={(e) => setResponsable(e.target.value)} maxLength={120} />
      </label>
      <label className="en-linea">
        <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} />
        Activo
      </label>

      <div className="acciones">
        <button className="boton primario" type="submit" disabled={guardando}>
          {guardando ? "Guardando…" : "Guardar"}
        </button>
        <button className="boton" type="button" onClick={alCancelar} disabled={guardando}>Cancelar</button>
        {definicion && alEliminar && (
          <button className="boton peligro" type="button" onClick={() => setConfirmarBorrado(true)} disabled={guardando}>
            Eliminar
          </button>
        )}
      </div>

      {confirmarBorrado && definicion && (
        <Confirmar
          titulo={`Eliminar ${definicion.codigo}`}
          textoConfirmar="Eliminar"
          peligro
          alConfirmar={eliminar}
          alCancelar={() => setConfirmarBorrado(false)}
        >
          <p>Se borra la definición, sus variables y el histórico asociado.</p>
        </Confirmar>
      )}
    </form>
  );
}
