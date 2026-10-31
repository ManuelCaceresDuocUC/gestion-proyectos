import { useState } from "react";
import type { NodoEdt } from "../../types/planificacion";
import ArbolEdt from "./ArbolEdt";
import EsquemaEdt from "./EsquemaEdt";
import "./edt.css";

type Modo = "LISTA" | "ESQUEMA" | "AMBOS";
const MODOS: Array<[Modo, string]> = [["AMBOS", "Lista y esquema"], ["LISTA", "Solo lista"], ["ESQUEMA", "Solo esquema"]];

interface Props {
  proyectoId: number;
  nombreProyecto: string;
  nodos: NodoEdt[];
  editable: boolean;
  alCambiar: () => void;
}

/** EDT en lista editable, esquema gráfico o ambos a la vez. */
export default function VistaEdt({ proyectoId, nombreProyecto, nodos, editable, alCambiar }: Props) {
  const [modo, setModo] = useState<Modo>("AMBOS");
  return (
    <div className="edt-vista">
      <div className="edt-barra">
        <h3 style={{ margin: 0 }}>Estructura de desglose del trabajo (EDT)</h3>
        <div className="edt-modos" role="group" aria-label="Vista de la EDT">
          {MODOS.map(([valor, texto]) => (
            <button key={valor} aria-pressed={modo === valor} onClick={() => setModo(valor)}>{texto}</button>
          ))}
        </div>
      </div>
      <div className={`edt-rejilla${modo === "AMBOS" ? " edt-doble" : ""}`}>
        {modo !== "ESQUEMA" && <ArbolEdt proyectoId={proyectoId} nodos={nodos} editable={editable} alCambiar={alCambiar} />}
        {modo !== "LISTA" && <EsquemaEdt nombreProyecto={nombreProyecto} nodos={nodos} />}
      </div>
    </div>
  );
}
