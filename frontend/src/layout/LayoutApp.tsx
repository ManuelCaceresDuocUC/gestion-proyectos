import { Suspense } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { cerrarSesion, esAdmin, usuarioActual } from "../auth/sesion";
import { EstadoCarga, MensajeError } from "./estados";
import { menuVisible } from "./menu";

function salir() {
  cerrarSesion();
  window.location.assign("/login");
}

export function LayoutApp() {
  const usuario = usuarioActual();
  if (!usuario) {
    return <MensajeError mensaje="No hay una sesión activa." />;
  }
  const grupos = menuVisible(esAdmin());
  return (
    <div className="app-marco">
      <header className="barra">
        <div>
          <p className="marca">Gestión de proyectos</p>
          <nav className="nav-modulos" aria-label="Módulos">
            {grupos.map((grupo) => (
              <div key={grupo.id} className="grupo-nav">
                <span className="grupo-nav-titulo">{grupo.titulo}</span>
                {grupo.entradas.map((entrada) => (
                  <NavLink
                    key={entrada.ruta}
                    to={entrada.ruta}
                    className={({ isActive }) => (isActive ? "activo" : undefined)}
                  >
                    {entrada.texto}
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>
        </div>
        <div className="sesion">
          <span>{usuario.nombre}</span>
          <span className="vacio">{usuario.email}</span>
          <span className="etiqueta">{usuario.rol}</span>
          <button type="button" className="boton chico" onClick={salir}>
            Salir
          </button>
        </div>
      </header>
      <main className="contenido">
        <Suspense fallback={<EstadoCarga />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  );
}

export function PaginaNoEncontrada() {
  return (
    <section>
      <h2>Página no encontrada</h2>
      <MensajeError mensaje="Esa ruta no existe." />
    </section>
  );
}
