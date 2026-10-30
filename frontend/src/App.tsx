import { NavLink, Route, Routes, Navigate } from "react-router-dom";
import LoginPagina from "./features/auth/LoginPagina";
import ProyectosPagina from "./features/proyectos/ProyectosPagina";
import GanttPagina from "./features/gantt/GanttPagina";
import RecursosPagina from "./features/recursos/RecursosPagina";
import KpiPagina from "./features/kpi/KpiPagina";
import HitosPagina from "./features/hitos/HitosPagina";
import CambiosPagina from "./features/cambios/CambiosPagina";
import TablerosPagina from "./features/tableros/TablerosPagina";

// TODO (C-03): guardas por rol (ADMIN administra todo; USUARIO_PROYECTO trabaja los proyectos sin administrar usuarios ni KPI) y layout definitivo.
const enlaces = [
  ["/proyectos", "Proyectos"],
  ["/gantt", "Gantt"],
  ["/recursos", "Recursos y costos"],
  ["/hitos", "Hitos"],
  ["/kpi", "KPI"],
  ["/cambios", "Cambios"],
  ["/tableros", "Tableros"],
];

export default function App() {
  return (
    <div style={{ fontFamily: "system-ui, sans-serif", padding: 16 }}>
      <nav style={{ display: "flex", gap: 16, marginBottom: 16 }}>
        {enlaces.map(([ruta, texto]) => (
          <NavLink key={ruta} to={ruta}>{texto}</NavLink>
        ))}
      </nav>
      <Routes>
        <Route path="/" element={<Navigate to="/proyectos" replace />} />
        <Route path="/login" element={<LoginPagina />} />
        <Route path="/proyectos" element={<ProyectosPagina />} />
        <Route path="/gantt" element={<GanttPagina />} />
        <Route path="/recursos" element={<RecursosPagina />} />
        <Route path="/hitos" element={<HitosPagina />} />
        <Route path="/kpi" element={<KpiPagina />} />
        <Route path="/cambios" element={<CambiosPagina />} />
        <Route path="/tableros" element={<TablerosPagina />} />
      </Routes>
    </div>
  );
}
