/** Mensaje de error de la API o de una guarda. Reutilizable en el layout. */
export function MensajeError({ mensaje }: { mensaje: string }) {
  if (!mensaje) {
    return null;
  }
  return (
    <p className="error" role="alert">
      {mensaje}
    </p>
  );
}

/** Estado de carga del layout. El mismo texto lo pueden usar las pantallas. */
export function EstadoCarga({ texto = "Cargando…" }: { texto?: string }) {
  return (
    <p className="vacio" role="status">
      {texto}
    </p>
  );
}
