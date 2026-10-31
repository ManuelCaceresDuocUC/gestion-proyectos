-- V22 (A-14): cada versión de la línea base guarda la fecha de fin y el costo total del proyecto en ese momento.
alter table linea_base
  add column fecha_fin   date,
  add column costo_total numeric(14,2) not null default 0;
