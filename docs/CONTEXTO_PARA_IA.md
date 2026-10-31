# Contexto para trabajar con una IA (Dev B y Dev C)

Este documento está pensado para **pegárselo (o adjuntárselo) a tu asistente de IA** antes de pedirle código. Reúne lo que ya existe en `main`,
cómo se conecta tu parte con el núcleo de planificación (Dev A) y las convenciones que hay que respetar. Si algo contradice al código, manda el código.

## 1. Qué es el proyecto
Sistema web para planificar y gestionar proyectos de software (ramo Gestión de Proyectos de Software). Stack: **Spring Boot 3.5 / Java 21 + PostgreSQL 16 + Flyway**
en `backend/`, y **React 18 + Vite + TypeScript** (react-router-dom 6, recharts) en `frontend/`. Se levanta con `docker compose up --build -d` (backend y base)
y `npm run dev` en `frontend`. Entrega: **jueves 5 de noviembre de 2026**. Congelamiento de funcionalidades propuesto: **3 de noviembre**.

La rúbrica evalúa sobre todo la integración (R8, 50 %): en la demo se **cambia una duración, se modifican recursos y se aprueba una solicitud**, y se
muestra que cambian el Gantt, los costos, los KPI y los dos tableros. Caso mínimo: 4 etapas, 12 actividades, 20 subactividades, 4 tipos de dependencia,
5 profesionales, 4 tipos de recurso, 6 hitos, 8 KPI de proyecto e hitos, 5 solicitudes de cambio, 12 KPI de cambio y 2 tableros.

## 2. Lo que ya está hecho (Dev A) y puedes usar
- **Proyectos y EDT** (`proyecto/`): proyecto con estado, presupuesto, fechas; EDT de 3 niveles (etapa > actividad > subactividad) con códigos 1, 1.1, 1.1.1.
- **Dependencias** FS, SS, FF y SF con desfase, rechazo de ciclos, y recálculo de fechas (`planificacion/`).
- **Línea base** y comparación plan/real. **Gantt** y pantallas de proyectos/EDT en el frontend.
- **Servicio de recálculo único**: `RecalculoService.recalcular(proyectoId)`.
- Documentación: `docs/modelo-datos.md`, `docs/arquitectura.md`, `docs/api/*.md`, `docs/gestion/decisiones/ADR-*.md`, `docs/requerimientos.md`.
- Proyecto de ejemplo: `scripts/demo-gantt.ps1` crea por la API un caso completo (sirve de modelo para el seed).

**La EDT es por entregables** (`docs/gestion/edt-entregables.md`): etapas y actividades con nombre de sustantivo, subactividades con verbo. Duración, recursos y
dependencias van en las **subactividades** (tareas sin hijos); etapas y actividades consolidan.

## 3. Reglas que no se negocian
1. **Un solo punto de recálculo.** Ningún módulo calcula fechas o totales por su cuenta. Si tu módulo cambia algo que afecta el plan (asignaciones, tarifas,
   aprobación de una solicitud), llama a `RecalculoService.recalcular(proyectoId)`.
2. **Migraciones Flyway**: una nueva por cambio, con tu rango (**B: V30+, C: V40+**; A usa V20+). **Nunca edites una migración ya publicada en `main`.**
   La mayoría de las tablas ya existen desde V1 (ver `docs/modelo-datos.md`): normalmente solo necesitas la entidad JPA, no una migración.
3. **Hibernate valida el esquema** (`ddl-auto: validate`): si el tipo de una columna no coincide con tu entidad, el backend no arranca. Trampas conocidas:
   `smallint` → `Short`, `numeric` → `BigDecimal`, `varchar(n)` → `String`, `char(n)` se evita (usa `varchar`), `timestamptz` → `Instant`.
   Evita en JPQL el patrón `(:p is null or campo = :p)`; usa métodos derivados o consultas separadas.
4. **Errores de negocio** con `ApiException` (`noEncontrado`, `invalido(codigo, mensaje)`, `conflicto(codigo, mensaje)`): se responden como
   `{ "codigo", "mensaje", "detalles" }` y el frontend muestra el `mensaje`. Códigos estables en MAYÚSCULAS.
5. **No acoples módulos por relaciones JPA**: guarda ids numéricos (`Long proyectoId`) y consulta por repositorio, como hace `Tarea`.
6. **Un proyecto `TERMINADO` no se modifica**: usa `ProyectoService.obtenerEditable(proyectoId)` antes de escribir.
7. **Las pruebas existentes deben seguir en verde** (`mvn test`). Si activas seguridad (C-01), agrega `@WithMockUser` a las pruebas de API existentes.

## 4. Cómo se conecta tu módulo con el recálculo
Cada módulo aporta un paso. Un paso es un `@Component` que implementa `PasoRecalculo` y declara su orden:

```java
package cl.proyecto.gestion.planificacion.recalculo; // la interfaz y OrdenRecalculo viven aquí

@Component
@Order(OrdenRecalculo.COSTOS)            // ver tabla de órdenes
public class PasoCostos implements PasoRecalculo {
    @Override
    public void ejecutar(Long proyectoId) {
        // lee asignaciones y tareas del proyecto, calcula, guarda con los repositorios
    }
}
```

Órdenes (`OrdenRecalculo`): `MOTOR_FECHAS = 10` (A), `CONSOLIDACION = 20` (A), **`COSTOS = 30` (B)**, `CONSOLIDACION_COSTOS = 35` (A),
**`SOBREASIGNACION = 40` (B)**, **`HITOS = 50` (B)**, **`KPI = 60` (B)**. Si falta una constante para tu paso, agrégala en `OrdenRecalculo`.

Contrato del recálculo:
- Todo corre en **una transacción**: si un paso lanza una excepción, se deshace todo. Una `ApiException` se devuelve tal cual; otra excepción responde `500 RECALCULO_FALLIDO`.
- **Costos (B)**: escribe `tarea.costo_plan` y `tarea.costo_real` **solo en las tareas sin hijos** (costo = cantidad × tarifa). Las etapas, actividades y el proyecto los suma
  el paso de Dev A (`CONSOLIDACION_COSTOS`) sin duplicar. No sumes padres e hijos.
- Los pasos deben ser **idempotentes**: ejecutarlos dos veces seguidas da el mismo resultado.
- **KPI (B)**: el paso guarda un registro en `kpi_historico` con el valor, el cumplimiento y el semáforo. Los tableros solo **leen** lo ya calculado.
- Para **leer** el plan: `TareaRepository.findByProyectoId(id)`, `ProyectoRepository`, `DependenciaRepository`. Las tareas con hijos se detectan con `TareaRepository.existsByParentId`.

### Cómo cambiar el plan desde tu módulo (Dev C: aplicar una solicitud aprobada)
No escribas fechas a mano. Cambia el dato de entrada y recalcula:
- Cambiar la duración de una tarea hoja: `CronogramaService.parchear(tareaId, new TareaPatch(nuevaDuracion, null, null, null))`. Devuelve `ResultadoTarea` con las fechas que se movieron.
- Crear o quitar una dependencia: `DependenciaService`.
- Cambiar costos o recursos: modifica las asignaciones (módulo de B) y llama a `RecalculoService.recalcular(proyectoId)`.
- La **línea base se conserva**: la solicitud aprobada solo mueve el plan vigente. Fijar la línea base es `LineaBaseService` / `POST /api/proyectos/{id}/linea-base`.

## 5. Patrones de código a imitar
Backend, por módulo en `backend/src/main/java/cl/proyecto/gestion/<modulo>/`:
- **Entidad** como `proyecto/Tarea.java` (campos públicos, `@Entity`, `@Table`, ids como `Long`). **Repositorio** `extends JpaRepository<Entidad, Long>` con métodos derivados.
- **Servicio** `@Service` con `@Transactional`; las reglas de negocio lanzan `ApiException`. Ejemplo completo: `proyecto/TareaService.java`.
- **Controlador** `@RestController` bajo `/api/...` con DTOs `record` y `@Valid`. Ejemplo: `proyecto/TareaController.java`. Documenta la API nueva en `docs/api/<modulo>.md`.
- **Dominio puro** (cálculos sin base de datos) en clases sin Spring, probadas con JUnit simple: ver `planificacion/motor/`. Úsalo para el evaluador de KPI.
- **Pruebas**: servicio con `@SpringBootTest @Transactional` (ver `proyecto/EdtServiceTest`); API con `@AutoConfigureMockMvc` (ver `planificacion/lineabase/LineaBaseApiTest`);
  integración completa por HTTP: `integracion/RecalculoDuracionE2ETest` (plantilla para B-18 y C-14).

Frontend, en `frontend/src/`:
- `api/client.ts`: `api<T>(ruta, opciones)` con prefijo `/api`; lanza `ErrorApi` con el `mensaje` del backend. Un archivo por módulo (`api/planificacion.ts` es el ejemplo).
- `types/`: tipos TypeScript que reflejan los JSON de la API.
- `features/<modulo>/`: página y componentes (`proyectos/` y `gantt/` son los ejemplos). Estilos globales en `estilos.css`; clases reutilizables: `.boton`, `.tarjeta`, `.tabla`, `.error`, `.aviso`, `.etiqueta`, `.progreso`.
- Permisos por rol: hoy `features/proyectos/permisos.ts` y `features/gantt/permisos.ts` devuelven `true`. **Dev C**: al tener el login, haz que lean el rol real (ADMIN / USUARIO_PROYECTO).
- Las rutas ya existen en `App.tsx` (`/recursos`, `/hitos`, `/kpi`, `/cambios`, `/tableros`) con páginas vacías: reemplaza el contenido de la tuya.

## 6. Datos que ya existen en la base (V1–V22)
Las tablas de tus módulos ya están creadas: `tipo_recurso`, `recurso`, `asignacion`, `hito`, `hito_tarea`, `kpi_definicion`, `kpi_variable`, `kpi_historico`,
`solicitud_cambio`, `cambio_etapa`, `cambio_historial`, `evidencia`, `usuario`, `no_laborable`. Columnas, tipos y restricciones: **`docs/modelo-datos.md`**.
Solo agrega una migración (V30+/V40+) si necesitas cambiar una tabla; si no, solo crea la entidad.

## 7. Qué hacer en cada módulo (resumen y orden sugerido)
Los issues completos están en el tablero de GitHub (pestaña Projects). Orden recomendado por valor para la demo:

**Dev B** — recursos, costos, hitos, KPI, tablero de proyecto
1. B-05/B-06: CRUD de tipos de recurso, recursos (profesionales) y asignaciones a subactividades (cantidad y tarifa).
2. B-08 + B-16: `PasoCostos` (orden 30) y `PasoSobreasignacion` (40): costo plan y real por tarea hoja y alerta de profesionales sobreasignados. **Este es el recálculo 2 de la demo.**
3. B-11: hitos (duración 0, asociados a tareas; no pueden ir antes de que terminen sus actividades) y `PasoHitos` (50).
4. B-12/B-13/B-14: evaluador de KPI (fórmula en texto, variables, meta, umbrales, "No calculable" si el denominador es 0), los 8 KPI y el histórico (`PasoKpi`, 60).
5. B-21: tablero de proyecto (recharts), leyendo lo ya calculado.
6. Después: B-07 (calendario; ya existe `no_laborable` y el calendario en `planificacion/calendario`), B-10/B-15 (pantallas), B-17 (KPI de cambio).

**Dev C** — autenticación, gestión del cambio, tablero de cambio, seed, documentación
1. C-03/C-04/C-08: solicitudes de cambio (CRUD, filtros), las 10 etapas del cambio y transiciones de estado con aprobar/rechazar.
2. C-09: aplicar una solicitud aprobada (sección 4): cambia el plan por los servicios de A y dispara el recálculo; **es el recálculo 3 de la demo.**
3. C-07/C-11: **seed** del caso mínimo. Recomendación: un `CommandLineRunner` activado por `APP_SEED_ENABLED` que cree los datos **usando los servicios** (`ProyectoService`, `TareaService`, `DependenciaService`, `LineaBaseService`), para que las fechas salgan del motor. `scripts/demo-gantt.ps1` muestra el orden: proyecto → EDT → dependencias → línea base.
4. C-01: JWT y los roles `ADMIN` y `USUARIO_PROYECTO` (ADMIN administra usuarios, KPI y aprueba cambios). Hazlo en una rama aparte y actualiza las pruebas existentes.
5. C-12: tablero de cambio. Después, C-05/C-06/C-10 (evidencias y pantallas) y la documentación (C-16, C-17).

## 8. Flujo de trabajo (Git)
- Una rama por issue o grupo corto: `feat/B-05-recursos`. PR pequeños y frecuentes; **el CI (backend y frontend) debe estar en verde**.
- Auto-merge al tener el CI verde (sin revisión obligatoria). Commits y PR en español, con `Closes #<n° del issue>`.
- Antes de empezar: `git pull` en `main`. Si dos personas tocan el mismo archivo, resuelvan en el chat; las zonas compartidas son `App.tsx`, `OrdenRecalculo.java` y `docs/`.
- Las decisiones se toman en el chat del equipo; 24 horas de silencio equivalen a aprobación.
- Prueba local: `docker compose up --build -d`, `cd backend; mvn test`, `cd frontend; npm run build`.

## 9. Prompt sugerido para tu IA
> Estás ayudándome en un proyecto Spring Boot 3.5 / Java 21 + React 18 + TypeScript. Te adjunto `docs/CONTEXTO_PARA_IA.md`, `docs/modelo-datos.md`, `docs/arquitectura.md`
> y el issue que debo implementar. Respeta estrictamente las reglas de la sección 3 y los patrones de la sección 5. No cambies código de otros módulos salvo agregar
> constantes en `OrdenRecalculo`. Para cada tarea: (1) entidad y repositorio, (2) servicio con reglas y `ApiException`, (3) controlador y DTOs, (4) pruebas JUnit,
> (5) documentación de la API en `docs/api/`, (6) el frontend con el cliente `api<T>()`. Si necesitas ver un archivo existente, pídemelo antes de inventar su contenido.

**Archivos que conviene adjuntar junto con este documento:** `docs/modelo-datos.md`, `docs/arquitectura.md`, el issue, `proyecto/Tarea.java`, `proyecto/TareaService.java`,
`planificacion/recalculo/PasoConsolidacionCostos.java`, `planificacion/recalculo/OrdenRecalculo.java`, `planificacion/lineabase/LineaBaseApiTest.java` y `frontend/src/api/client.ts`.
