import type { AlertaSobreasignacion, TipoRecurso } from "../../types/recursos";

export function esProfesional(tipo: TipoRecurso | undefined): boolean {
  return !!tipo && tipo.nombre.toLowerCase().includes("profesional");
}

export function horas(valor: number): string {
  return `${new Intl.NumberFormat("es-CL", { maximumFractionDigits: 2 }).format(valor)} h`;
}

export function numero(valor: string): number {
  return Number(valor.replace(",", "."));
}

/** Texto corto para la marca de la tarea: profesionales y días en que se pasa de capacidad. */
export function resumenSobreasignacion(alertas: AlertaSobreasignacion[], tareaId: number): string {
  const propias = alertas.filter((a) => a.tareas.some((t) => t.id === tareaId));
  if (propias.length === 0) return "";
  return propias
    .map((a) => `${a.profesional}: ${horas(a.cargaHoras)} el ${a.fecha} (capacidad ${horas(a.capacidadHorasDia)})`)
    .join("\n");
}

export function idsSobreasignados(alertas: AlertaSobreasignacion[]): Set<number> {
  return new Set(alertas.flatMap((a) => a.tareas.map((t) => t.id)));
}
