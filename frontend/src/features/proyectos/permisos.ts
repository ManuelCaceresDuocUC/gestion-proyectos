// Un solo lugar para decidir qué acciones se muestran según el rol.
// TODO (C-01): leer el rol del usuario autenticado. ADMIN y USUARIO_PROYECTO trabajan los proyectos; la
// administración de usuarios y de KPI es solo de ADMIN y no está en estas pantallas.
export function puedeEditar(): boolean {
  return true;
}
