export interface EntradaMenu {
  ruta: string;
  texto: string;
  soloAdmin?: boolean;
}

export interface GrupoMenu {
  id: string;
  titulo: string;
  entradas: EntradaMenu[];
}

/** Rutas ya registradas en App. KPI es administración (B-15): solo ADMIN. */
const menu: GrupoMenu[] = [
  {
    id: "planificacion",
    titulo: "Planificación",
    entradas: [
      { ruta: "/proyectos", texto: "Proyectos" },
      { ruta: "/gantt", texto: "Gantt" },
      { ruta: "/importar", texto: "Importar Excel" },
    ],
  },
  {
    id: "recursos",
    titulo: "Recursos",
    entradas: [
      { ruta: "/recursos", texto: "Recursos y costos" },
      { ruta: "/hitos", texto: "Hitos" },
    ],
  },
  {
    id: "cambio",
    titulo: "Cambio",
    entradas: [{ ruta: "/cambios", texto: "Cambios" }],
  },
  {
    id: "seguimiento",
    titulo: "Seguimiento",
    entradas: [
      { ruta: "/tableros", texto: "Tableros" },
      { ruta: "/kpi", texto: "KPI", soloAdmin: true },
    ],
  },
];

export function menuVisible(admin: boolean): GrupoMenu[] {
  return menu
    .map((grupo) => ({
      ...grupo,
      entradas: grupo.entradas.filter((entrada) => admin || !entrada.soloAdmin),
    }))
    .filter((grupo) => grupo.entradas.length > 0);
}
