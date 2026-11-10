# API de KPI (T-02 / B-12…B-17)

Tablas `kpi_definicion`, `kpi_variable` y `kpi_historico` ya existen (V1; V21 agrega columnas con default que esta API no escribe).
Variables permitidas en la fórmula: [kpi-variables.md](kpi-variables.md).
Evaluación: paso 60 del recálculo, no un `POST` de “calcular”.

B-13 carga KP-01…KP-08 como definiciones globales (`proyecto_id` nulo) la primera vez que corre el paso, con la fórmula y la meta del seed. KP-01 queda con `meta` nula: al evaluar, la meta es `AVANCE_PLAN × 100`. B-14 guarda el snapshot y publica la lectura. El alta y la edición de definiciones siguen en B-15.

Unidad `%`: la fórmula guardada es una razón. `EvaluadorKpi` multiplica por 100 una vez
antes de redondear a 2 decimales. `días` e `índice` no se escalan. Las fórmulas KC del seed
no llevan `* 100`.

B-17 deja el catálogo `CatalogoKpiCambio` (KC-01…KC-13, iguales al JSON) y
`VariablesCambioService`, que arma las variables desde las solicitudes y etapas ya guardadas.
C-12: `asegurarCatalogo()` (solo en el recálculo, no en un GET) inserta también KC-01…KC-13
globales, igual que KP-01…KP-08. El tablero de cambio lee esos snapshots (`categoria` `CAMBIO`).
KC-11…KC-13 siguen propuestos; el issue #88 no tiene respuesta del profesor y aún no hay
un ADR específico para esa decisión.

C-01: **escribir** definiciones es solo `ADMIN` (`403 PROHIBIDO` para `USUARIO_PROYECTO`).
Leer histórico y listado: cualquier autenticado.

## Rutas

`SecurityConfig` (C-01) reserva **POST/PUT/PATCH/DELETE `/api/kpi/**`** a ADMIN. Las escrituras
van bajo `/kpi`, no bajo `/proyectos/{id}/kpi` (ese path no entra al matcher). Esas escrituras siguen pendientes (B-15).

| Método | Ruta | Notas |
|---|---|---|
| GET | `/proyectos/{proyectoId}/kpi` | autenticado. Definiciones activas con el último snapshot. `alerta` y `alertaMotivo` (`ROJO` o `EMPEORA`) |
| POST | `/kpi` | pendiente B-15. ADMIN |
| GET | `/kpi/{id}` | pendiente B-15 |
| PUT | `/kpi/{id}` | pendiente B-15. ADMIN |
| DELETE | `/kpi/{id}` | pendiente B-15. ADMIN |
| GET | `/kpi/{id}/historico?proyectoId=` | autenticado. Serie del proyecto, más reciente primero. Sin `proyectoId`: `400 PROYECTO_OBLIGATORIO` |

`alertaMotivo` es `ROJO` si el último semáforo es rojo, o `EMPEORA` si el cumplimiento bajó respecto al snapshot anterior. El primer snapshot solo alerta en rojo. `valor` nulo es «No calculable» (`noCalculable: true`, sin semáforo).

No hay endpoint de recálculo: un cambio de plan (duración, recurso, aplicar TIEMPO) dispara el paso KPI.

## Campos de definición (modelo)

`codigo` único por proyecto (o global si `proyectoId` es nulo), `nombre`, `categoria`
(`PROYECTO`, `HITOS`, `CAMBIO`), `ambito` / periodicidad / fuente (V21), `formula`, `unidad`,
`sentido` (`MAYOR_MEJOR`, `MENOR_MEJOR`, rango V21), `operadorMeta`
(`MAYOR_IGUAL`, `MENOR_IGUAL`, `MAYOR`, `MENOR`, `ENTRE`), `meta`, `metaMax`,
`umbralVerde` (default 1), `umbralAmarillo` (default 0.8), `responsable`, `activo`,
opcional `hitoId` / vínculo a tarea (V21).

Histórico: `valor` nulo = No calculable; `cumplimiento`; `semaforo` (`VERDE`, `AMARILLO`, `ROJO`);
`motivo`; `calculadoEn`.

Caso mínimo: 8 KPI de proyecto/hitos (KP-01…08) y ≥ 12 de cambio (KC-01…13 en el JSON).
`KpiService` carga KP-01…08 y KC-01…13 en el recálculo (21 definiciones globales).
`GET /proyectos/{id}/kpi` lista las 21. El seed de C **no** inserta definiciones.

B publica el JSON concreto con B-12; no duplicar lógica de fórmulas en el front ni en los tableros.
