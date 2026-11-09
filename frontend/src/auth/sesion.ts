/** Sesión C-01: token y usuario en sessionStorage. El front no es frontera de seguridad. */

export type RolUsuario = "ADMIN" | "USUARIO_PROYECTO";

export interface UsuarioSesion {
  id: number;
  email: string;
  nombre: string;
  rol: RolUsuario;
}

const TOKEN = "gestion.token";
const USUARIO = "gestion.usuario";
const EVENTO = "gestion-sesion";

function avisar(): void {
  window.dispatchEvent(new Event(EVENTO));
}

/** Para que el menú se entere al guardar o cerrar sesión. */
export function alCambiarSesion(escuchar: () => void): () => void {
  window.addEventListener(EVENTO, escuchar);
  return () => window.removeEventListener(EVENTO, escuchar);
}

export function tokenActual(): string | null {
  return sessionStorage.getItem(TOKEN);
}

export function usuarioActual(): UsuarioSesion | null {
  const raw = sessionStorage.getItem(USUARIO);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as UsuarioSesion;
  } catch {
    return null;
  }
}

export function haySesion(): boolean {
  return Boolean(tokenActual() && usuarioActual());
}

export function guardarSesion(token: string, usuario: UsuarioSesion): void {
  sessionStorage.setItem(TOKEN, token);
  sessionStorage.setItem(USUARIO, JSON.stringify(usuario));
  avisar();
}

export function cerrarSesion(): void {
  sessionStorage.removeItem(TOKEN);
  sessionStorage.removeItem(USUARIO);
  avisar();
}

export function esAdmin(): boolean {
  return usuarioActual()?.rol === "ADMIN";
}

export function estaAutenticado(): boolean {
  const rol = usuarioActual()?.rol;
  return rol === "ADMIN" || rol === "USUARIO_PROYECTO";
}
