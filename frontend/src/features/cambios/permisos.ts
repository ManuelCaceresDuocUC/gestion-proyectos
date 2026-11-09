import { esAdmin, estaAutenticado } from "../../auth/sesion";

export function puedeEditar(): boolean {
  return estaAutenticado();
}

/** Aprobar y rechazar: solo ADMIN. El API también lo exige. */
export function puedeAprobar(): boolean {
  return esAdmin();
}
