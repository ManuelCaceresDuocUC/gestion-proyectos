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
    <section className="tarjeta" style={{ maxWidth: 360 }}>
      <h2>Ingreso</h2>
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
          <input
            type="password"
            autoComplete="current-password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            required
          />
        </label>
        <div className="acciones">
          <button className="boton primario" type="submit" disabled={enviando}>
            {enviando ? "Ingresando…" : "Ingresar"}
          </button>
        </div>
      </form>
    </section>
  );
}
