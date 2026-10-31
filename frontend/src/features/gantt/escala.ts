// Utilidades de fechas y escala del Gantt. Las fechas viajan como "yyyy-MM-dd" y se trabajan como número de día (UTC).
export const MS_DIA = 86_400_000;

export type Escala = "DIA" | "SEMANA";
/** Píxeles por día en cada escala. */
export const PX_DIA: Record<Escala, number> = { DIA: 26, SEMANA: 9 };

export function aDia(fecha: string): number {
  const [a, m, d] = fecha.slice(0, 10).split("-").map(Number);
  return Math.round(Date.UTC(a, m - 1, d) / MS_DIA);
}

export function aIso(dia: number): string {
  return new Date(dia * MS_DIA).toISOString().slice(0, 10);
}

/** Día de la semana de un número de día: 0 = domingo ... 6 = sábado. */
export function diaSemana(dia: number): number {
  return new Date(dia * MS_DIA).getUTCDay();
}

export function hoyIso(): string {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

export function nombreMes(dia: number): string {
  const d = new Date(dia * MS_DIA);
  return `${MESES[d.getUTCMonth()]} ${d.getUTCFullYear()}`;
}

export function diaDelMes(dia: number): number {
  return new Date(dia * MS_DIA).getUTCDate();
}

export function etiquetaCorta(dia: number): string {
  const d = new Date(dia * MS_DIA);
  return `${String(d.getUTCDate()).padStart(2, "0")}/${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
}
