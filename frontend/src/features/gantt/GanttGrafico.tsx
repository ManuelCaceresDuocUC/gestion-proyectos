import type { DependenciaDto } from "../../types/planificacion";
import { PX_DIA, aDia, diaDelMes, diaSemana, etiquetaCorta, nombreMes } from "./escala";
import type { Escala } from "./escala";
import type { FilaGantt } from "./modelo";

export const ALTO_FILA = 30;
export const ALTO_ENCABEZADO = 44;

interface Props {
  filas: FilaGantt[];
  dependencias: DependenciaDto[];
  escala: Escala;
  /** Fecha "hoy" (yyyy-MM-dd) para el marcador y la regla de atraso. */
  hoy: string;
  seleccionada: number | null;
  alSeleccionar: (id: number) => void;
}

interface Tramo { x1: number; x2: number }

/** Diagrama de Gantt en SVG: barras jerárquicas, avance, línea base, atrasos, hitos, dependencias y marcador de hoy. */
export default function GanttGrafico({ filas, dependencias, escala, hoy, seleccionada, alSeleccionar }: Props) {
  const px = PX_DIA[escala];

  const dias: number[] = [aDia(hoy)];
  for (const f of filas) {
    for (const v of [f.nodo.inicioPlan, f.nodo.finPlan, f.comp?.inicioBase, f.comp?.finBase, f.comp?.inicioReal, f.comp?.finReal]) {
      if (v) dias.push(aDia(v));
    }
  }
  const primero = Math.min(...dias) - 3;
  const ultimo = Math.max(...dias) + 4;
  const ancho = (ultimo - primero + 1) * px;
  const alto = ALTO_ENCABEZADO + filas.length * ALTO_FILA;
  const xDe = (dia: number) => (dia - primero) * px;
  const xHoy = xDe(aDia(hoy)) + px / 2;

  // Posición horizontal de cada tarea visible (para barras y flechas).
  const tramos = new Map<number, Tramo>();
  const filaDe = new Map<number, number>();
  filas.forEach((f, i) => {
    filaDe.set(f.nodo.id, i);
    if (f.nodo.inicioPlan && f.nodo.finPlan) {
      const x1 = xDe(aDia(f.nodo.inicioPlan));
      tramos.set(f.nodo.id, { x1, x2: f.esHito ? x1 : xDe(aDia(f.nodo.finPlan) + 1) });
    }
  });

  // Encabezado: meses arriba y días (o lunes) abajo.
  const meses: Array<{ x: number; texto: string }> = [];
  const marcas: Array<{ x: number; texto: string; finDeSemana: boolean }> = [];
  for (let d = primero; d <= ultimo; d++) {
    const x = xDe(d);
    if (d === primero || diaDelMes(d) === 1) meses.push({ x, texto: nombreMes(d) });
    const dow = diaSemana(d);
    if (escala === "DIA") marcas.push({ x, texto: String(diaDelMes(d)), finDeSemana: dow === 0 || dow === 6 });
    else if (dow === 1) marcas.push({ x, texto: etiquetaCorta(d), finDeSemana: false });
  }

  function flecha(d: DependenciaDto) {
    const p = tramos.get(d.predecesoraId);
    const s = tramos.get(d.sucesoraId);
    const ip = filaDe.get(d.predecesoraId);
    const is = filaDe.get(d.sucesoraId);
    if (!p || !s || ip === undefined || is === undefined) return null;
    const desdeFin = d.tipo === "FS" || d.tipo === "FF";
    const haciaInicio = d.tipo === "FS" || d.tipo === "SS";
    const xp = desdeFin ? p.x2 : p.x1;
    const xs = haciaInicio ? s.x1 : s.x2;
    const yp = ALTO_ENCABEZADO + ip * ALTO_FILA + ALTO_FILA / 2;
    const ys = ALTO_ENCABEZADO + is * ALTO_FILA + ALTO_FILA / 2;
    const salida = xp + (desdeFin ? 8 : -8);
    const entrada = xs + (haciaInicio ? -8 : 8);
    const medio = ys > yp ? ys - ALTO_FILA / 2 : ys + ALTO_FILA / 2;
    const ruta = desdeFin && haciaInicio && salida <= entrada
      ? `M${xp},${yp} H${salida} V${ys} H${xs}`
      : `M${xp},${yp} H${salida} V${medio} H${entrada} V${ys} H${xs}`;
    return (
      <path key={d.id} d={ruta} className="g-flecha" markerEnd="url(#punta)">
        <title>{`${d.tipo}${d.desfaseDias ? ` ${d.desfaseDias > 0 ? "+" : ""}${d.desfaseDias} d` : ""}`}</title>
      </path>
    );
  }

  return (
    <svg width={ancho} height={alto} className="g-svg" role="img" aria-label="Diagrama de Gantt">
      <defs>
        <marker id="punta" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="7" markerHeight="7" orient="auto">
          <path d="M0,0 L8,4 L0,8 z" className="g-punta" />
        </marker>
      </defs>

      {/* fondo: fines de semana (escala diaria) y líneas de las marcas */}
      {marcas.map((m) => (
        <g key={m.x}>
          {m.finDeSemana && <rect x={m.x} y={ALTO_ENCABEZADO} width={px} height={alto - ALTO_ENCABEZADO} className="g-finde" />}
          <line x1={m.x} x2={m.x} y1={ALTO_ENCABEZADO - 12} y2={alto} className="g-grilla" />
          <text x={m.x + px / 2} y={ALTO_ENCABEZADO - 15 + 12} className="g-marca" textAnchor="middle">{m.texto}</text>
        </g>
      ))}
      {meses.map((m) => (
        <text key={m.x} x={m.x + 4} y={14} className="g-mes">{m.texto}</text>
      ))}
      {filas.map((f, i) => (
        <rect key={f.nodo.id} x={0} y={ALTO_ENCABEZADO + i * ALTO_FILA} width={ancho} height={ALTO_FILA}
          className={f.nodo.id === seleccionada ? "g-fila g-fila-sel" : "g-fila"} onClick={() => alSeleccionar(f.nodo.id)} />
      ))}

      {/* barras */}
      {filas.map((f, i) => {
        const y = ALTO_ENCABEZADO + i * ALTO_FILA;
        const t = tramos.get(f.nodo.id);
        const c = f.comp;
        const base = c?.inicioBase && c.finBase
          ? { x1: xDe(aDia(c.inicioBase)), x2: f.esHito ? xDe(aDia(c.inicioBase)) : xDe(aDia(c.finBase) + 1) }
          : null;
        const atrasada = c?.atrasada ?? false;
        const avance = Math.max(0, Math.min(100, f.nodo.avance));
        const detalle = `${f.nodo.codigo} ${f.nodo.nombre} · ${f.nodo.inicioPlan ?? "?"} → ${f.nodo.finPlan ?? "?"} · ${Math.round(avance)} %`
          + (c?.desviacionFinDias ? ` · ${c.desviacionFinDias > 0 ? "+" : ""}${c.desviacionFinDias} d vs base` : "")
          + (atrasada ? " · ATRASADA" : "");
        return (
          <g key={f.nodo.id} onClick={() => alSeleccionar(f.nodo.id)} className="g-barra-grupo">
            {base && !f.esHito && <rect x={base.x1} y={y + ALTO_FILA - 8} width={Math.max(2, base.x2 - base.x1)} height={4} className="g-base"><title>Línea base</title></rect>}
            {base && f.esHito && <rect x={base.x1 - 4} y={y + ALTO_FILA - 10} width={8} height={8} className="g-base" transform={`rotate(45 ${base.x1} ${y + ALTO_FILA - 6})`}><title>Línea base</title></rect>}
            {t && f.esHito && (
              <rect x={t.x1 - 7} y={y + ALTO_FILA / 2 - 7 - 2} width={14} height={14}
                transform={`rotate(45 ${t.x1} ${y + ALTO_FILA / 2 - 2})`}
                className={atrasada ? "g-hito g-atrasada" : "g-hito"}><title>{detalle}</title></rect>
            )}
            {t && !f.esHito && f.esResumen && (
              <rect x={t.x1} y={y + 8} width={Math.max(2, t.x2 - t.x1)} height={8} rx={2}
                className={atrasada ? "g-resumen g-atrasada" : "g-resumen"}><title>{detalle}</title></rect>
            )}
            {t && !f.esHito && !f.esResumen && (
              <>
                <rect x={t.x1} y={y + 6} width={Math.max(2, t.x2 - t.x1)} height={14} rx={3}
                  className={atrasada ? "g-barra g-atrasada" : "g-barra"}><title>{detalle}</title></rect>
                <rect x={t.x1} y={y + 6} width={Math.max(0, ((t.x2 - t.x1) * avance) / 100)} height={14} rx={3} className="g-avance" />
              </>
            )}
            {c?.inicioReal && (
              <rect x={xDe(aDia(c.inicioReal))} y={y + 2} height={3} className="g-real"
                width={Math.max(2, xDe(aDia(c.finReal ?? hoy) + 1) - xDe(aDia(c.inicioReal)))}><title>Ejecución real</title></rect>
            )}
          </g>
        );
      })}

      {dependencias.map(flecha)}

      {/* marcador de hoy */}
      <line x1={xHoy} x2={xHoy} y1={ALTO_ENCABEZADO - 12} y2={alto} className="g-hoy" />
      <text x={xHoy + 3} y={ALTO_ENCABEZADO - 15 + 12 + 12} className="g-hoy-texto">hoy</text>
    </svg>
  );
}
