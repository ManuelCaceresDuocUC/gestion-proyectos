create table sobreasignacion_alerta (
  id                    bigserial primary key,
  proyecto_id           bigint not null references proyecto(id) on delete cascade,
  recurso_id            bigint not null references recurso(id),
  recurso_nombre        varchar(160) not null,
  fecha                 date not null,
  carga_horas           numeric(12,4) not null,
  capacidad_horas_dia   numeric(4,1) not null,
  exceso_horas          numeric(12,4) not null,
  unique (proyecto_id, recurso_id, fecha)
);

create table sobreasignacion_alerta_tarea (
  alerta_id    bigint not null references sobreasignacion_alerta(id) on delete cascade,
  tarea_id     bigint not null references tarea(id) on delete cascade,
  tarea_codigo varchar(20) not null,
  tarea_nombre varchar(200) not null,
  primary key (alerta_id, tarea_id)
);

create index ix_sobreasignacion_alerta_proyecto
  on sobreasignacion_alerta(proyecto_id, fecha);
