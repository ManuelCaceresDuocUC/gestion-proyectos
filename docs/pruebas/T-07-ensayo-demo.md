# T-07 · Ensayo de la demo y video de respaldo

Issue #130. Coordinación: Dev C. Participan A, B y C.
Guion: `docs/pruebas/guion-demo.md`. Casos: `docs/pruebas/casos-de-prueba.md`.

Corte de este documento: **10 nov 2026**. Lo de abajo es el plan y lo ya listo en `main`; **no** afirma que el ensayo o el video ya se hicieron.

## Criterios (#130)

| Criterio | Estado |
|---|---|
| Ensayo completo con cronómetro (~15 min) desde reset, en el entorno desplegado | **Por confirmar** (fecha y hora más adelante) |
| Video de respaldo grabado y guardado **fuera del repo** | **Pendiente** |
| Plan B si falla internet o Render: local con Docker Compose | Escrito abajo |

## Ya listo (no espera al ensayo)

- Los 3 recálculos tienen E2E en `main`: A-18, B-18 (#186), C-14.
- CP-02 ya tiene capturas: `docs/pruebas/capturas/cp-02-*.png`.
- Front Render: https://gestion-proyectos-frontend.onrender.com
- API health: https://gestion-proyectos-api.onrender.com/api/health
- `/kpi` sin pantalla de administración (B-15 abierto): en la demo no se inventan números.

## Por confirmar más adelante

| Ítem | Quién | Nota |
|---|---|---|
| Fecha y hora de la llamada | Los 3 | No hay horario cerrado |
| Ensayo cronometrado en Render | Los 3 | Reset si `EEC-2026` no está limpio |
| Video (~15 min), fuera de GitHub | C coordina | Drive/OneDrive; no subir el mp4 al repo |
| Capturas CP-01 (A) y CP-03 (C) | A y C | Carpeta `docs/pruebas/capturas/` cuando existan |

## Cómo grabar el video (cuando toque)

1. Seed limpio. En Render: redeploy o reset que acuerde B. En local: `docker compose down -v` y `$env:APP_SEED_ENABLED = "true"` (en esta máquina el API suele estar en **8081**, no 8080).
2. Entrar como `admin@demo.cl`. La clave está en `db/seed/caso-prueba.json`; no se dicta en el video.
3. Seguir el guion minuto a minuto. Recálculo 3: `SC-01` → tipo Tiempo → evaluar → aprobar (plan igual) → aplicar (3.1.1 a 10 d).
4. Cierre: `usuario@demo.cl` no ve Aprobar/Rechazar.
5. Guardar el archivo fuera del repo y anotar el enlace aquí cuando exista.

Enlace del video: **pendiente**.

## Plan B (local)

PowerShell, carpeta del repo (equivalente a `scripts/reset-demo.sh`):

```powershell
docker compose down -v
$env:APP_SEED_ENABLED = "true"
docker compose up -d --build
```

Si el 8080 está ocupado: API en 8081 y Vite con `$env:API_PROXY = "http://localhost:8081"`.

## Obtenido (este corte)

Sin ensayo fechado. Sin video. Sin capturas nuevas de CP-01/CP-03. Este archivo deja el criterio y el plan B escritos; el cierre de #130 espera el ensayo y el video.
