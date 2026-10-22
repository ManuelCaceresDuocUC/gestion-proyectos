# Seed: caso de prueba

Datos ficticios para la demo y las pruebas. Se empaquetan en el backend como `classpath:seed/`.

- `caso-prueba.json`: proyecto "Habilitar un espacio de estudio colaborativo" (derivado de
  `Actividad_Gantt_Hitos.xlsx`) con **4 etapas, 12 actividades, 20 subactividades, 35 dependencias de los
  4 tipos (FS, SS, FF, SF), 5 profesionales, 4 tipos de recurso, 6 hitos y 5 solicitudes de cambio**.
- `kpi-definiciones.json`: 8 KPI de proyecto e hitos y 13 KPI de gestión del cambio (los 10 de
  `Plantilla_Seguimiento_Gestion_del_Cambio_KPI.xlsx` más 3 adicionales propuestos).

Notas:
- `referencia_semana` es solo orientativa; las fechas reales las calcula el motor a partir de las dependencias.
- El seed debe poder recargarse en un segundo (`scripts/reset-demo.sh`) y crecer en cada sprint (C-09, C-13).
- Contraseñas de los usuarios demo: solo para desarrollo local.
