# API de KPI (esqueleto T-02 / B-12…B-17)

Pendiente de implementación. Tablas `kpi_definicion`, `kpi_variable` y `kpi_historico` ya existen.
Variables permitidas en la fórmula: [kpi-variables.md](kpi-variables.md).
Evaluación: paso 60 del recálculo, no un `POST` de “calcular”.

C-01: **escribir** definiciones es solo `ADMIN` (`403 PROHIBIDO` para `USUARIO_PROYECTO`).
Leer histórico y listado: cualquier autenticado.

## Rutas previstas

`SecurityConfig` (C-01) reserva **POST/PUT/PATCH/DELETE `/api/kpi/**`** a ADMIN. Las escrituras
van bajo `/kpi`, no bajo `/proyectos/{id}/kpi` (ese path no entra al matcher).

| Método | Ruta | Notas |
|---|---|---|
| GET | `/proyectos/{proyectoId}/kpi` | listado (autenticado) |
| POST | `/kpi` | 201. ADMIN. Body incluye `proyectoId`. Fórmula con variables del catálogo |
| GET | `/kpi/{id}` | ficha |
| PUT | `/kpi/{id}` | ADMIN |
| DELETE | `/kpi/{id}` | ADMIN |
| GET | `/kpi/{id}/historico` | snapshots, más reciente primero |

No hay endpoint de recálculo: un cambio de plan (duración, recurso, aplicar TIEMPO) dispara
el paso KPI cuando exista.

## Campos de definición (modelo)

`codigo` único por proyecto (o global si `proyectoId` es nulo), `nombre`, `categoria`
(`PROYECTO`, `HITOS`, `CAMBIO`), `ambito` / periodicidad / fuente (V21), `formula`, `unidad`,
`sentido` (`MAYOR_MEJOR`, `MENOR_MEJOR`, rango V21), `operadorMeta`
(`MAYOR_IGUAL`, `MENOR_IGUAL`, `MAYOR`, `MENOR`, `ENTRE`), `meta`, `metaMax`,
`umbralVerde` (default 1), `umbralAmarillo` (default 0.8), `responsable`, `activo`,
opcional `hitoId` / vínculo a tarea (V21).

Histórico: `valor` nulo = No calculable; `cumplimiento`; `semaforo` (`VERDE`, `AMARILLO`, `ROJO`);
`motivo`; `calculadoEn`.

Caso mínimo: 8 KPI de proyecto/hitos (KP-01…08) y ≥ 12 de cambio (KC-01…13 en el seed).
El seed JSON **no se carga** hasta `KpiService` (C-11 lo dejó explícito).

B publica el JSON concreto con B-12; no duplicar lógica de fórmulas en el front ni en los tableros.
