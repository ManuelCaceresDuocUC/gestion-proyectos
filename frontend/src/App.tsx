import { NavLink, Route, Routes, Navigate } from "react-router-dom";
import { useEffect, useState, type ReactNode } from "react";
import { alCambiarSesion, cerrarSesion, haySesion, usuarioActual } from "./auth/sesion";
import LoginPagina from "./features/auth/LoginPagina";
import ProyectosPagina from "./features/proyectos/ProyectosPagina";
import GanttPagina from "./features/gantt/GanttPagina";
import RecursosPagina from "./features/recursos/RecursosPagina";
import KpiPagina from "./features/kpi/KpiPagina";
import HitosPagina from "./features/hitos/HitosPagina";
import CambiosPagina from "./features/cambios/CambiosPagina";
import TablerosPagina from "./features/tableros/TablerosPagina";

// Layout definitivo: B-04. Aquí solo se exige sesión y un Salir mínimo.
const enlaces = [
  ["/proyectos", "Proyectos"],
  ["/gantt", "Gantt"],
  ["/recursos", "Recursos y costos"],
  ["/hitos", "Hitos"],
  ["/kpi", "KPI"],
  ["/cambios", "Cambios"],
  ["/tableros", "Tableros"],
];

function Privada({ children }: { children: ReactNode }) {
  return haySesion() ? <>{children}</> : <Navigate to="/login" replace />;
}

function salir() {
  cerrarSesion();
  window.location.assign("/login");
}

export default function App() {
  const [, setTick] = useState(0);
  useEffect(() => alCambiarSesion(() => setTick((n) => n + 1)), []);
  const usuario = usuarioActual();
  return (
    <div style={{ fontFamily: "system-ui, sans-serif", padding: 16 }}>
      <nav style={{ display: "flex", gap: 16, marginBottom: 16, alignItems: "center", flexWrap: "wrap" }}>
        {enlaces.map(([ruta, texto]) => (
          <NavLink key={ruta} to={ruta}>{texto}</NavLink>
        ))}
        {usuario && (
          <>
            <span className="vacio">{usuario.email}</span>
            <button type="button" className="boton chico" onClick={salir}>Salir</button>
          </>
        )}
      </nav>
      <Routes>
        <Route path="/" element={<Navigate to="/proyectos" replace />} />
        <Route path="/login" element={<LoginPagina />} />
        <Route path="/proyectos" element={<Privada><ProyectosPagina /></Privada>} />
        <Route path="/gantt" element={<Privada><GanttPagina /></Privada>} />
        <Route path="/recursos" element={<Privada><RecursosPagina /></Privada>} />
        <Route path="/hitos" element={<Privada><HitosPagina /></Privada>} />
        <Route path="/kpi" element={<Privada><KpiPagina /></Privada>} />
        <Route path="/cambios" element={<Privada><CambiosPagina /></Privada>} />
        <Route path="/tableros" element={<Privada><TablerosPagina /></Privada>} />
      </Routes>
    </div>
  );
}
