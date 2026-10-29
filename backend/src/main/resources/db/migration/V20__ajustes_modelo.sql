-- V20: ajustes del modelo detectados al revisar V1 contra Hibernate (ddl-auto: validate). Dueño: Dev A.
-- Rangos de migraciones: A usa V20+, B usa V30+, C usa V40+. Nunca se edita una migración ya publicada.

-- char(3) lo reporta PostgreSQL como bpchar y Hibernate espera varchar para un String: sin este
-- cambio el backend no arranca en cuanto se mapea la entidad Proyecto.
alter table proyecto alter column moneda type varchar(3);
