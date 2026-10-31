import type { NodoEdt } from "../../types/planificacion";

const ANCHO = 200;
const ALTO = 46;
const SEP_X = 52;
const SEP_Y = 12;

type Clase = "RAIZ" | "ETAPA" | "ACTIVIDAD" | "SUBACTIVIDAD";

interface Caja {
  clave: string;
  codigo: string;
  nombre: string;
  clase: Clase;
  x: number;
  y: number;
  padre: Caja | null;
}

function recortar(texto: string, max: number): string {
  return texto.length > max ? `${texto.slice(0, max - 1)}…` : texto;
}

/** Calcula posiciones: cada hoja ocupa una fila y cada padre se centra respecto de sus hijos. */
function distribuir(nombreProyecto: string, nodos: NodoEdt[]): Caja[] {
  const cajas: Caja[] = [];
  let siguienteFila = 0;

  function poner(n: NodoEdt, nivel: number, padre: Caja): Caja {
    const caja: Caja = {
      clave: String(n.id), codigo: n.codigo, nombre: n.nombre, clase: n.tipo,
      x: nivel * (ANCHO + SEP_X), y: 0, padre,
    };
    cajas.push(caja);
    if (n.hijos.length === 0) {
      caja.y = siguienteFila++ * (ALTO + SEP_Y);
    } else {
      const hijas = n.hijos.map((h) => poner(h, nivel + 1, caja));
      caja.y = (hijas[0].y + hijas[hijas.length - 1].y) / 2;
    }
    return caja;
  }

  const raiz: Caja = { clave: "raiz", codigo: "", nombre: nombreProyecto, clase: "RAIZ", x: 0, y: 0, padre: null };
  cajas.push(raiz);
  if (nodos.length === 0) return cajas;
  const etapas = nodos.map((n) => poner(n, 1, raiz));
  raiz.y = (etapas[0].y + etapas[etapas.length - 1].y) / 2;
  return cajas;
}

interface Props {
  nombreProyecto: string;
  nodos: NodoEdt[];
}

/** Esquema de la EDT (proyecto > etapas > actividades > subactividades). Se redibuja solo al cambiar el árbol. */
export default function EsquemaEdt({ nombreProyecto, nodos }: Props) {
  const cajas = distribuir(nombreProyecto, nodos);
  const ancho = Math.max(...cajas.map((c) => c.x)) + ANCHO + 4;
  const alto = Math.max(...cajas.map((c) => c.y)) + ALTO + 4;

  return (
    <div className="esq">
      <h4>Esquema de la EDT</h4>
      <div className="esq-niveles">
        <span className="n1">Etapa</span><span className="n2">Actividad</span><span className="n3">Subactividad</span>
      </div>
      <svg width={ancho} height={alto} role="img" aria-label="Esquema de la estructura de desglose del trabajo">
        {cajas.filter((c) => c.padre).map((c) => {
          const p = c.padre as Caja;
          const x1 = p.x + ANCHO;
          const y1 = p.y + ALTO / 2 + 2;
          const x2 = c.x;
          const y2 = c.y + ALTO / 2 + 2;
          const m = x1 + SEP_X / 2;
          return <path key={`e-${c.clave}`} className="esq-enlace" d={`M${x1},${y1} H${m} V${y2} H${x2}`} />;
        })}
        {cajas.map((c) => (
          <g key={c.clave} transform={`translate(${c.x},${c.y + 2})`}>
            <rect className={`esq-caja esq-caja-${c.clase}`} width={ANCHO} height={ALTO} rx={8}>
              <title>{c.codigo ? `${c.codigo} ${c.nombre}` : c.nombre}</title>
            </rect>
            {c.codigo && <text className={`esq-cod esq-txt-${c.clase}`} x={10} y={18}>{c.codigo}</text>}
            <text className={`esq-nom esq-txt-${c.clase}`} x={10} y={c.codigo ? 35 : 28}>
              {recortar(c.nombre, c.codigo ? 27 : 26)}
            </text>
          </g>
        ))}
      </svg>
      {nodos.length === 0 && <p className="vacio">Agregue etapas y el esquema se irá dibujando.</p>}
    </div>
  );
}
