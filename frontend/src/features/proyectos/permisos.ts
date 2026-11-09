import { estaAutenticado } from "../../auth/sesion";

// ADMIN y USUARIO_PROYECTO trabajan los proyectos; usuarios y KPI son de ADMIN (fuera de estas pantallas).
export function puedeEditar(): boolean {
  return estaAutenticado();
}
