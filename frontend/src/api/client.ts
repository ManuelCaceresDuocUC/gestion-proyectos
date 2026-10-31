// Cliente HTTP base. Cada módulo agrega su archivo en src/api/ (proyectos.ts, recursos.ts, ...).
// TODO (issue C-02): adjuntar el JWT en Authorization y redirigir a /login ante un 401.

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

export async function api<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const respuesta = await fetch(`/api${ruta}`, {
    ...opciones,
    headers: { "Content-Type": "application/json", ...((opciones.headers as Record<string, string> | undefined) ?? {}) },
  });
  if (!respuesta.ok) {
    let cuerpo: { codigo?: string; mensaje?: string; detalles?: string[] } = {};
    try {
      cuerpo = (await respuesta.json()) as typeof cuerpo;
    } catch {
      /* respuesta sin JSON */
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
