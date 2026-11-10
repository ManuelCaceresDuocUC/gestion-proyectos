import { useEffect, useState, type ReactNode } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { alCambiarSesion, esAdmin, haySesion } from "../auth/sesion";
import { EstadoCarga, MensajeError } from "./estados";

/** Sin sesión no entra al marco. La sesión se lee de sessionStorage (C-01). */
export function SesionRequerida() {
  const [lista, setLista] = useState(false);
  const [, setTick] = useState(0);
  useEffect(() => {
    setLista(true);
    return alCambiarSesion(() => setTick((n) => n + 1));
  }, []);
  if (!lista) {
    return <EstadoCarga texto="Comprobando sesión…" />;
  }
  return haySesion() ? <Outlet /> : <Navigate to="/login" replace />;
}

/** Administración de KPI: el rol se comprueba también si se escribe la URL. */
export function SoloAdmin({ children }: { children: ReactNode }) {
  if (esAdmin()) {
    return <>{children}</>;
  }
  return (
    <section>
      <h2>Acceso denegado</h2>
      <MensajeError mensaje="No tiene permiso para administrar los KPI." />
    </section>
  );
}
