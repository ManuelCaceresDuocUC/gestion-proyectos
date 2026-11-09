// Cliente HTTP. C-01: Bearer desde sessionStorage; 401 limpia sesión y va a /login (sin bucle).
import { cerrarSesion, tokenActual } from "../auth/sesion";

/** Error de la API: conserva el código y el mensaje que devuelve el backend ({codigo, mensaje, detalles}). */
export class ErrorApi extends Error {
  constructor(
    public readonly estado: number,
    public readonly codigo: string,
    mensaje: string,
    public readonly detalles: string[] = [],
  ) {
    super(mensaje);
    this.name = "ErrorApi";
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_URL ?? "").replace(/\/+$/, "");
const LOGIN = "/auth/login";

export async function api<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const esLogin = ruta === LOGIN;
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((opciones.headers as Record<string, string> | undefined) ?? {}),
  };
  const token = esLogin ? null : tokenActual();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const respuesta = await fetch(`${apiBaseUrl}/api${ruta}`, {
    ...opciones,
    headers,
  });
  if (!respuesta.ok) {
    let cuerpo: { codigo?: string; mensaje?: string; detalles?: string[] } = {};
    try {
      cuerpo = (await respuesta.json()) as typeof cuerpo;
    } catch {
      /* respuesta sin JSON */
    }
    if (respuesta.status === 401) {
      cerrarSesion();
      if (!esLogin && window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    throw new ErrorApi(
      respuesta.status,
      cuerpo.codigo ?? "ERROR",
      cuerpo.mensaje ?? `Error ${respuesta.status} en ${ruta}`,
      cuerpo.detalles ?? [],
    );
  }
  return respuesta.status === 204 ? (undefined as T) : ((await respuesta.json()) as T);
}
