// TODO (C-01): leer el rol del usuario autenticado.
export function puedeEditar(): boolean {
  return true;
}

/** Aprobar y rechazar: en C-01 solo ADMIN. Hoy las rutas están abiertas. */
export function puedeAprobar(): boolean {
  return true;
}
