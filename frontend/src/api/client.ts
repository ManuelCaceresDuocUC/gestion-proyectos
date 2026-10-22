// Cliente HTTP base. Cada módulo agrega su archivo en src/api/ (proyectos.ts, recursos.ts, ...).
// TODO (issue C-02): adjuntar el JWT en Authorization y redirigir a /login ante un 401.
export async function api<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const respuesta = await fetch(`/api${ruta}`, {
    ...opciones,
    headers: { "Content-Type": "application/json", ...((opciones.headers as Record<string, string> | undefined) ?? {}) },
  });
  if (!respuesta.ok) {
    throw new Error(`Error ${respuesta.status} en ${ruta}`);
  }
  return respuesta.status === 204 ? (undefined as T) : ((await respuesta.json()) as T);
}
