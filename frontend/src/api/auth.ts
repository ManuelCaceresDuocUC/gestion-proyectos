import { api } from "./client";
import type { UsuarioSesion } from "../auth/sesion";

export interface LoginRespuesta {
  token: string;
  usuario: UsuarioSesion;
}

export const authApi = {
  login: (email: string, contrasena: string) =>
    api<LoginRespuesta>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, contrasena }),
    }),
};
