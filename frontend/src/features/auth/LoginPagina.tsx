import { useState } from "react";
import type { FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { authApi } from "../../api/auth";
import { ErrorApi } from "../../api/client";
import { guardarSesion, haySesion } from "../../auth/sesion";
import { mensajeDe } from "../proyectos/formato";

export default function LoginPagina() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [verClave, setVerClave] = useState(false);

  if (haySesion()) {
    return <Navigate to="/proyectos" replace />;
  }

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setEnviando(true);
    try {
      const r = await authApi.login(email.trim(), contrasena);
      guardarSesion(r.token, r.usuario);
      navigate("/proyectos", { replace: true });
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : mensajeDe(err));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="login-pantalla">
      <section className="tarjeta login-tarjeta">
        <p className="marca">Gestión de proyectos</p>
        <h1>Ingreso</h1>
        <p className="login-ayuda">Usa tu correo institucional. Hay dos roles: administrador y usuario de proyecto.</p>
        {error && <p className="error">{error}</p>}
        <form className="formulario" onSubmit={(e) => void enviar(e)}>
          <label>
            Correo
            <input
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label>
            Contraseña
            <span className="login-clave">
              <input
                type={verClave ? "text" : "password"}
                autoComplete="current-password"
                value={contrasena}
                onChange={(e) => setContrasena(e.target.value)}
                required
              />
              <button
                type="button"
                className="boton chico"
                aria-pressed={verClave}
                aria-label={verClave ? "Ocultar contraseña" : "Mostrar contraseña"}
                onClick={() => setVerClave((v) => !v)}
              >
                {verClave ? "Ocultar" : "Mostrar"}
              </button>
            </span>
          </label>
          <button className="boton primario login-enviar" type="submit" disabled={enviando}>
            {enviando ? "Ingresando…" : "Ingresar"}
          </button>
        </form>
      </section>
    </main>
  );
}
