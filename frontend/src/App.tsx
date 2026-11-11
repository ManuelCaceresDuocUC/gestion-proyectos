import { Navigate, Route, Routes } from "react-router-dom";
import LoginPagina from "./features/auth/LoginPagina";
import ProyectosPagina from "./features/proyectos/ProyectosPagina";
import GanttPagina from "./features/gantt/GanttPagina";
import RecursosPagina from "./features/recursos/RecursosPagina";
import KpiPagina from "./features/kpi/KpiPagina";
import HitosPagina from "./features/hitos/HitosPagina";
import CambiosPagina from "./features/cambios/CambiosPagina";
import TablerosPagina from "./features/tableros/TablerosPagina";
import ImportarPagina from "./features/importacion/ImportarPagina";
import { LayoutApp, PaginaNoEncontrada } from "./layout/LayoutApp";
import { SesionRequerida, SoloAdmin } from "./layout/guardas";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPagina />} />
      <Route element={<SesionRequerida />}>
        <Route element={<LayoutApp />}>
          <Route path="/" element={<Navigate to="/proyectos" replace />} />
          <Route path="/proyectos" element={<ProyectosPagina />} />
          <Route path="/gantt" element={<GanttPagina />} />
          <Route path="/recursos" element={<RecursosPagina />} />
          <Route path="/hitos" element={<HitosPagina />} />
          <Route path="/cambios" element={<CambiosPagina />} />
          <Route path="/importar" element={<ImportarPagina />} />
          <Route path="/tableros" element={<TablerosPagina />} />
          <Route path="/kpi" element={<SoloAdmin><KpiPagina /></SoloAdmin>} />
          <Route path="*" element={<PaginaNoEncontrada />} />
        </Route>
      </Route>
    </Routes>
  );
}
