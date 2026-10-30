-- V21 (A-21): alinea el modelo con la "Guía de Proyecto Integrador" del profesor.
-- Son cambios aditivos o de reglas: no se pierde ningún dato (las tablas aún no tienen datos reales).

-- 1. Proyecto: estados Planificado, En ejecución, Suspendido, Terminado (antes CERRADO)
alter table proyecto drop constraint if exists proyecto_estado_check;
update proyecto set estado = 'TERMINADO' where estado = 'CERRADO';
alter table proyecto add constraint proyecto_estado_check
  check (estado in ('PLANIFICADO','EN_EJECUCION','SUSPENDIDO','TERMINADO'));

-- 2. Roles: administrador y usuario de proyecto (antes VISUALIZADOR)
alter table usuario drop constraint if exists usuario_rol_check;
update usuario set rol = 'USUARIO_PROYECTO' where rol = 'VISUALIZADOR';
alter table usuario add constraint usuario_rol_check check (rol in ('ADMIN','USUARIO_PROYECTO'));

-- 3. Recursos: campos que pide la guía (especialidad, horas/semana, categoría, cantidad requerida, detalle)
alter table recurso
  add column especialidad        varchar(120),                  -- profesionales
  add column categoria           varchar(60),                   -- tipo de profesional
  add column horas_semana        numeric(5,1) check (horas_semana is null or horas_semana between 0 and 168),
  add column cantidad_requerida  numeric(8,2) check (cantidad_requerida is null or cantidad_requerida >= 0),
  add column detalle             varchar(250);                  -- licencias: software y período; materiales: unidad; etc.
alter table asignacion
  add column horas_semana numeric(5,1) check (horas_semana is null or horas_semana >= 0),
  add column semanas      numeric(5,1) check (semanas is null or semanas >= 0);   -- costo profesional = horas_semana x semanas x tarifa

-- 4. Hitos: responsable
alter table hito add column responsable_id bigint references recurso(id);

-- 5. KPI: ámbito, objetivo, periodicidad, fuente (automática o manual) y sentido "rango"
alter table kpi_definicion drop constraint if exists kpi_definicion_sentido_check;
alter table kpi_definicion add constraint kpi_definicion_sentido_check
  check (sentido in ('MAYOR_MEJOR','MENOR_MEJOR','RANGO'));
alter table kpi_definicion
  add column ambito       varchar(15) not null default 'PROYECTO'
                          check (ambito in ('PROYECTO','ETAPA','ACTIVIDAD','HITO','CAMBIO')),
  add column objetivo     text,
  add column periodicidad varchar(15) check (periodicidad in ('DIARIA','SEMANAL','MENSUAL','POR_EVENTO')),
  add column fuente       varchar(15) not null default 'AUTOMATICA' check (fuente in ('AUTOMATICA','MANUAL')),
  add column hito_id      bigint references hito(id) on delete cascade,     -- KPI asociado a un hito
  add column tarea_id     bigint references tarea(id) on delete cascade;    -- KPI de una etapa o actividad

-- 6. Solicitudes de cambio: tipos y estados de la guía; la aprobación pasa a ser parte del estado
alter table solicitud_cambio drop constraint if exists solicitud_cambio_estado_check;
alter table solicitud_cambio drop constraint if exists solicitud_cambio_tipo_check;
alter table solicitud_cambio alter column estado type varchar(20);
update solicitud_cambio set estado = case
    when aprobacion = 'APROBADA'  and estado in ('ABIERTO','SUSPENDIDO') then 'APROBADO'
    when aprobacion = 'RECHAZADA' or estado = 'CANCELADO'                then 'RECHAZADO'
    when estado = 'EN_CURSO'                                              then 'EN_IMPLEMENTACION'
    when estado = 'SUSPENDIDO'                                            then 'EN_EVALUACION'
    else estado end;
update solicitud_cambio set tipo = case tipo
    when 'PERSONAS' then 'ORGANIZACION'
    when 'PROCESO'  then 'ORGANIZACION'
    when 'OTRO'     then 'ALCANCE'
    else tipo end;
alter table solicitud_cambio drop column aprobacion;
alter table solicitud_cambio add constraint solicitud_cambio_estado_check
  check (estado in ('ABIERTO','EN_EVALUACION','APROBADO','RECHAZADO','EN_IMPLEMENTACION','CERRADO'));
alter table solicitud_cambio add constraint solicitud_cambio_tipo_check
  check (tipo in ('ALCANCE','TIEMPO','COSTO','RECURSO','TECNOLOGIA','ORGANIZACION'));
alter table solicitud_cambio
  add column impacto_alcance      text,
  add column impacto_recursos     text,
  add column resultado_evaluacion varchar(15) check (resultado_evaluacion in ('EFECTIVO','PARCIAL','NO_EFECTIVO'));
