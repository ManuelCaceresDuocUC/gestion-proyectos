-- V1: modelo de datos inicial. BORRADOR del Sprint 0: el issue A-01 lo revisa y completa entre los 3.
-- Regla: nunca se edita una migración ya subida a main; los cambios van en V2, V3, ...

create table usuario (
  id            bigserial primary key,
  nombre        varchar(120) not null,
  email         varchar(160) not null unique,
  password_hash varchar(100) not null,
  rol           varchar(20)  not null check (rol in ('ADMIN','VISUALIZADOR')),
  activo        boolean      not null default true,
  creado_en     timestamptz  not null default now()
);

create table tipo_recurso (
  id     bigserial primary key,
  nombre varchar(60) not null unique,          -- Profesional, Equipamiento, Material, Servicio externo
  unidad varchar(20) not null                  -- hora, unidad, global
);

create table recurso (
  id                  bigserial primary key,
  tipo_recurso_id     bigint       not null references tipo_recurso(id),
  nombre              varchar(160) not null,
  tarifa              numeric(14,2) not null check (tarifa >= 0),   -- por hora (profesional) o costo unitario
  capacidad_horas_dia numeric(4,1),                                  -- solo profesionales
  activo              boolean not null default true
);

create table proyecto (
  id               bigserial primary key,
  codigo           varchar(30)  not null unique,
  nombre           varchar(160) not null,
  descripcion      text,
  estado           varchar(20)  not null default 'PLANIFICADO' check (estado in ('PLANIFICADO','EN_EJECUCION','CERRADO')),
  responsable_id   bigint references recurso(id),
  presupuesto      numeric(14,2) not null default 0 check (presupuesto >= 0),
  moneda           char(3) not null default 'CLP',
  dias_laborales   varchar(7) not null default 'LMXJV',
  fecha_inicio     date,
  fecha_fin_plan   date,
  fecha_fin_base   date,
  fecha_fin_real   date,
  avance           numeric(5,2) not null default 0 check (avance between 0 and 100),
  costo_plan       numeric(14,2) not null default 0,
  costo_real       numeric(14,2) not null default 0,
  creado_en        timestamptz not null default now(),
  cerrado_en       timestamptz
);

create table no_laborable (
  id          bigserial primary key,
  proyecto_id bigint references proyecto(id) on delete cascade,   -- null = aplica a todos los proyectos
  fecha       date not null,
  descripcion varchar(120),
  unique (proyecto_id, fecha)
);

create table linea_base (
  id          bigserial primary key,
  proyecto_id bigint not null references proyecto(id) on delete cascade,
  version     int    not null,
  motivo      varchar(200),
  creada_en   timestamptz not null default now(),
  unique (proyecto_id, version)
);

-- EDT: etapa > actividad > subactividad en una sola tabla jerárquica
create table tarea (
  id             bigserial primary key,
  proyecto_id    bigint not null references proyecto(id) on delete cascade,
  parent_id      bigint references tarea(id) on delete cascade,
  tipo           varchar(15) not null check (tipo in ('ETAPA','ACTIVIDAD','SUBACTIVIDAD')),
  codigo         varchar(20) not null,                 -- 1, 1.1, 1.1.1
  nombre         varchar(200) not null,
  descripcion    text,
  orden          int not null default 0,
  responsable_id bigint references recurso(id),
  duracion_dias  int check (duracion_dias >= 0),       -- hábiles; en padres se deriva de los hijos
  inicio_plan    date, fin_plan    date,
  inicio_base    date, fin_base    date,               -- línea base (copia al aprobar el plan)
  inicio_real    date, fin_real    date,
  avance         numeric(5,2) not null default 0 check (avance between 0 and 100),
  costo_plan     numeric(14,2) not null default 0,
  costo_real     numeric(14,2) not null default 0,
  costo_base     numeric(14,2),
  unique (proyecto_id, codigo),
  check ((tipo = 'ETAPA' and parent_id is null) or (tipo <> 'ETAPA' and parent_id is not null))
);
create index ix_tarea_proyecto on tarea(proyecto_id);
create index ix_tarea_parent on tarea(parent_id);

create table dependencia (
  id             bigserial primary key,
  predecesora_id bigint not null references tarea(id) on delete cascade,
  sucesora_id    bigint not null references tarea(id) on delete cascade,
  tipo           varchar(2) not null check (tipo in ('FS','SS','FF','SF')),
  desfase_dias   int not null default 0,                -- negativo = adelanto
  check (predecesora_id <> sucesora_id),
  unique (predecesora_id, sucesora_id)
);
create index ix_dependencia_sucesora on dependencia(sucesora_id);

create table asignacion (
  id            bigserial primary key,
  tarea_id      bigint not null references tarea(id) on delete cascade,
  recurso_id    bigint not null references recurso(id),
  cantidad      numeric(12,2) not null check (cantidad > 0),  -- horas (profesional) o unidades
  tarifa        numeric(14,2) not null check (tarifa >= 0),   -- tarifa vigente al asignar
  costo_plan    numeric(14,2) not null default 0,
  cantidad_real numeric(12,2) not null default 0,
  costo_real    numeric(14,2) not null default 0,
  unique (tarea_id, recurso_id)
);
create index ix_asignacion_recurso on asignacion(recurso_id);

create table hito (
  id                    bigserial primary key,
  proyecto_id           bigint not null references proyecto(id) on delete cascade,
  codigo                varchar(10) not null,
  logro                 varchar(250) not null,           -- logro verificable (duración cero)
  fecha_plan            date,
  fecha_base            date,
  fecha_real            date,
  estado                varchar(15) not null default 'PENDIENTE' check (estado in ('PENDIENTE','CUMPLIDO','NO_CUMPLIDO')),
  evidencia             text,                            -- evidencia y fuente de datos
  indicador             text,                            -- indicador / forma de cálculo
  criterio_aceptacion   text,                            -- unidad y criterio de aceptación
  validador             varchar(160),                    -- quién valida y cuándo
  accion_si_no_cumple   text,
  unique (proyecto_id, codigo)
);

create table hito_tarea (
  hito_id  bigint not null references hito(id) on delete cascade,
  tarea_id bigint not null references tarea(id) on delete cascade,
  primary key (hito_id, tarea_id)
);

-- KPI configurables (proyecto, hitos y cambio)
create table kpi_definicion (
  id            bigserial primary key,
  proyecto_id   bigint references proyecto(id) on delete cascade,   -- null = definición global
  codigo        varchar(20) not null,
  nombre        varchar(200) not null,
  categoria     varchar(15) not null check (categoria in ('PROYECTO','HITOS','CAMBIO')),
  descripcion   text,
  formula       text not null,                       -- ej: VALOR_GANADO / COSTO_REAL
  unidad        varchar(20),
  sentido       varchar(15) not null check (sentido in ('MAYOR_MEJOR','MENOR_MEJOR')),
  operador_meta varchar(15) not null check (operador_meta in ('MAYOR_IGUAL','MENOR_IGUAL','MAYOR','MENOR','ENTRE')),
  meta          numeric(18,4),
  meta_max      numeric(18,4),                       -- solo para ENTRE
  umbral_verde    numeric(6,4) not null default 1.0, -- cumplimiento >= verde => VERDE
  umbral_amarillo numeric(6,4) not null default 0.8, -- >= amarillo => AMARILLO, si no ROJO
  responsable   varchar(120),
  activo        boolean not null default true
);
create unique index ux_kpi_definicion on kpi_definicion (coalesce(proyecto_id, 0), codigo);

create table kpi_variable (
  id          bigserial primary key,
  kpi_id      bigint not null references kpi_definicion(id) on delete cascade,
  nombre      varchar(60) not null,                  -- nombre usado en la fórmula
  descripcion varchar(200),
  unique (kpi_id, nombre)
);

create table kpi_historico (
  id           bigserial primary key,
  kpi_id       bigint not null references kpi_definicion(id) on delete cascade,
  proyecto_id  bigint not null references proyecto(id) on delete cascade,
  valor        numeric(18,4),                        -- null = No calculable (denominador cero)
  cumplimiento numeric(8,4),
  semaforo     varchar(10) check (semaforo in ('VERDE','AMARILLO','ROJO')),
  motivo       varchar(60),                          -- recálculo, cambio aprobado, manual
  calculado_en timestamptz not null default now()
);
create index ix_kpi_historico on kpi_historico(kpi_id, calculado_en);

-- Gestión del cambio
create table solicitud_cambio (
  id                   bigserial primary key,
  proyecto_id          bigint not null references proyecto(id) on delete cascade,
  codigo               varchar(20) not null,
  nombre               varchar(200) not null,
  descripcion          text,                          -- descripción y justificación
  solicitante          varchar(120),
  responsable_id       bigint references recurso(id),
  fecha_solicitud      date not null default current_date,
  inicio_previsto      date,
  fin_previsto         date,
  fin_real             date,
  prioridad            varchar(10) not null default 'MEDIA' check (prioridad in ('BAJA','MEDIA','ALTA','CRITICA')),
  tipo                 varchar(15) not null check (tipo in ('PROCESO','TECNOLOGIA','PERSONAS','OTRO')),
  riesgo               varchar(10) not null default 'MEDIO' check (riesgo in ('BAJO','MEDIO','ALTO','CRITICO')),
  impacto              varchar(10) not null default 'MEDIO' check (impacto in ('BAJO','MEDIO','ALTO')),
  estado               varchar(12) not null default 'ABIERTO' check (estado in ('ABIERTO','EN_CURSO','CERRADO','SUSPENDIDO','CANCELADO')),
  aprobacion           varchar(10) not null default 'PENDIENTE' check (aprobacion in ('PENDIENTE','APROBADA','RECHAZADA')),
  aprobada_en          timestamptz,
  tarea_id             bigint references tarea(id),   -- tarea afectada
  impacto_dias         int not null default 0,        -- delta de duración que se aplica al aprobar
  impacto_costo        numeric(14,2) not null default 0,
  aplicada             boolean not null default false,
  aplicada_en          timestamptz,
  presupuesto_previsto numeric(14,2),
  costo_real           numeric(14,2),
  accion_correctiva    text,
  resultado            text,
  creado_en            timestamptz not null default now(),
  unique (proyecto_id, codigo)
);

-- Las 10 etapas: Necesidad, Evaluación, Riesgos, Planificación, Comunicación inicial,
-- Implementación, Seguimiento, Resultados corto plazo, Correctivas, Adopción cultural
create table cambio_etapa (
  id             bigserial primary key,
  solicitud_id   bigint not null references solicitud_cambio(id) on delete cascade,
  numero         smallint not null check (numero between 1 and 10),
  nombre         varchar(60) not null,
  estado         varchar(12) not null default 'PENDIENTE' check (estado in ('PENDIENTE','EN_CURSO','COMPLETADO','BLOQUEADO','NO_APLICA')),
  actualizado_en timestamptz not null default now(),
  actualizado_por bigint references usuario(id),
  unique (solicitud_id, numero)
);

create table cambio_historial (
  id             bigserial primary key,
  solicitud_id   bigint not null references solicitud_cambio(id) on delete cascade,
  campo          varchar(40) not null,
  valor_anterior text,
  valor_nuevo    text,
  usuario_id     bigint references usuario(id),
  comentario     text,
  fecha          timestamptz not null default now()
);
create index ix_cambio_historial on cambio_historial(solicitud_id, fecha);

create table evidencia (
  id           bigserial primary key,
  solicitud_id bigint not null references solicitud_cambio(id) on delete cascade,
  descripcion  varchar(250) not null,
  url          varchar(500),
  creado_por   bigint references usuario(id),
  creado_en    timestamptz not null default now()
);
