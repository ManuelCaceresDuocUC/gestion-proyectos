import { estaAutenticado } from "../../auth/sesion";

// C-01: ambos roles autenticados pueden editar el cronograma. El API es la frontera.
export function puedeEditarCronograma(): boolean {
  return estaAutenticado();
}
