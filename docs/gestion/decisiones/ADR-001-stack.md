# ADR-001: Stack tecnológico

- **Estado:** propuesto (confirmar en T-01)
- **Contexto:** 3 desarrolladores, 2 semanas, trabajo remoto; el ramo evalúa la gestión y la integración de módulos.
- **Decisión propuesta:** monolito modular Spring Boot 3 (Java 21) + PostgreSQL (Flyway) + React/Vite/TypeScript.
  Un único servicio de recálculo. Despliegue simple (Render/Railway + Vercel).
- **Alternativas:** Next.js + Prisma (un solo lenguaje); microservicios (descartado: complejidad innecesaria).
- **Consecuencias:** un solo despliegue y transacciones simples; los módulos se separan por paquetes y dueños.
