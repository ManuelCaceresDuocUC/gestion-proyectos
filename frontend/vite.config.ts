import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// En desarrollo, /api se redirige al backend. Si el 8080 está ocupado (p. ej. Jenkins),
// arranca Spring en otro puerto y: $env:API_PROXY='http://localhost:8081'
const backend = process.env.API_PROXY ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { "/api": backend } },
});
