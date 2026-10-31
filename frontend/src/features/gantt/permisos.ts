// A-17: solo ADMIN edita duraciones y dependencias desde el Gantt.
// TODO (C-01): devolver true solo si el usuario autenticado tiene rol ADMIN.
export function puedeEditarCronograma(): boolean {
  return true;
}
