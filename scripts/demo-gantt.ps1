# Crea por la API un proyecto de ejemplo con el tamano del caso minimo: 4 etapas (entregables), 12 actividades,
# 20 subactividades, los 4 tipos de dependencia, un hito, avances, linea base y un cambio de duracion con desviaciones.
# Requisitos: base arriba y backend corriendo en http://localhost:8080 (docker compose up --build -d, o mvn spring-boot:run).
# Ejecutar desde la raiz del repo:  powershell -ExecutionPolicy Bypass -File .\scripts\demo-gantt.ps1
$ErrorActionPreference = "Stop"
$api = "http://localhost:8080/api"

function Enviar($metodo, $ruta, $obj) {
    $json = $obj | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Method $metodo -Uri "$api$ruta" -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($json))
}

$codigo = "GANTT-" + (Get-Date -Format "HHmmss")
$p = Enviar Post "/proyectos" @{ codigo = $codigo; nombre = "Sistema de gestion de proyectos (ejemplo)"; presupuesto = 5000000; moneda = "CLP"; fechaInicio = "2026-11-02" }
$id = $p.id
function Nueva($padre, $nombre, $dias) {
    $cuerpo = @{ nombre = $nombre }
    if ($null -ne $padre) { $cuerpo.parentId = $padre.id }
    if ($null -ne $dias) { $cuerpo.duracionDias = $dias }
    Enviar Post "/proyectos/$id/tareas" $cuerpo
}
function Dep($pred, $suc, $tipo, $desfase) {
    Enviar Post "/proyectos/$id/dependencias" @{ predecesoraId = $pred.id; sucesoraId = $suc.id; tipo = $tipo; desfaseDias = $desfase } | Out-Null
}

# EDT orientada a entregables: etapa y actividad son sustantivos (lo que se entrega); la subactividad es el trabajo (verbo).
# Las dependencias van entre subactividades (tareas sin hijos).
$t = @{}   # codigo -> tarea creada
function Etapa($cod, $nombre) { $script:t[$cod] = Nueva $null $nombre $null }
function Act($cod, $padre, $nombre) { $script:t[$cod] = Nueva $script:t[$padre] $nombre $null }
function Sub($cod, $padre, $nombre, $dias) { $script:t[$cod] = Nueva $script:t[$padre] $nombre $dias }

Etapa "1" "Especificacion de requisitos"
Act "1.1" "1" "Catalogo de requisitos"
Sub "1.1.1" "1.1" "Entrevistar a usuarios" 3
Sub "1.1.2" "1.1" "Redactar requisitos" 2
Act "1.2" "1" "Modelo de datos"
Sub "1.2.1" "1.2" "Disenar modelo entidad-relacion" 3
Sub "1.2.2" "1.2" "Validar modelo con el docente" 1
Act "1.3" "1" "Plan de pruebas"
Sub "1.3.1" "1.3" "Redactar casos de prueba" 2

Etapa "2" "Diseno aprobado"
Act "2.1" "2" "Arquitectura del sistema"
Sub "2.1.1" "2.1" "Definir la arquitectura" 3
Sub "2.1.2" "2.1" "Documentar decisiones (ADR)" 2
Act "2.2" "2" "Prototipos de interfaz"
Sub "2.2.1" "2.2" "Dibujar prototipos" 4
Sub "2.2.2" "2.2" "Revisar prototipos con usuarios" 1
Act "2.3" "2" "Diseno de base de datos"
Sub "2.3.1" "2.3" "Escribir migraciones" 3

Etapa "3" "Sistema desarrollado y probado"
Act "3.1" "3" "Modulo backend"
Sub "3.1.1" "3.1" "Implementar la API" 6
Sub "3.1.2" "3.1" "Implementar el motor de planificacion" 5
Act "3.2" "3" "Modulo frontend"
Sub "3.2.1" "3.2" "Construir las pantallas" 5
Sub "3.2.2" "3.2" "Construir el Gantt" 4
Act "3.3" "3" "Informe de pruebas"
Sub "3.3.1" "3.3" "Ejecutar pruebas integradas" 3

Etapa "4" "Sistema entregado y documentado"
Act "4.1" "4" "Manual de usuario"
Sub "4.1.1" "4.1" "Redactar el manual" 2
Sub "4.1.2" "4.1" "Tomar capturas" 1
Act "4.2" "4" "Sistema desplegado"
Sub "4.2.1" "4.2" "Desplegar en el servidor" 2
Sub "4.2.2" "4.2" "Entregar al docente (hito)" 0
Act "4.3" "4" "Informe de cierre"
Sub "4.3.1" "4.3" "Redactar el informe de cierre" 2

# Dependencias de los 4 tipos: Dep predecesora sucesora tipo desfase
function D($p, $s, $tipo, $desfase) { Dep $script:t[$p] $script:t[$s] $tipo $desfase }
D "1.1.1" "1.1.2" "FS" 0
D "1.1.2" "1.2.1" "FS" 0
D "1.2.1" "1.2.2" "FS" 0
D "1.1.2" "1.3.1" "FS" 0
D "1.2.2" "2.1.1" "FS" 0
D "2.1.1" "2.1.2" "FS" 0
D "2.1.1" "2.2.1" "SS" 1     # prototipos empiezan 1 dia despues que la arquitectura
D "2.2.1" "2.2.2" "FS" 0
D "2.2.2" "2.3.1" "FS" 0
D "2.3.1" "3.1.1" "FS" 0
D "2.2.2" "3.2.1" "FS" 0
D "3.1.1" "3.1.2" "SS" 2
D "3.1.2" "3.2.2" "FF" 0     # el Gantt termina junto con el motor
D "3.2.1" "3.2.2" "FS" 0
D "3.1.2" "3.3.1" "FS" 0
D "3.2.2" "3.3.1" "FS" 0
D "3.3.1" "4.1.1" "FS" 0
D "4.1.1" "4.1.2" "FS" 0
D "3.3.1" "4.2.1" "FS" 0
D "4.2.1" "4.2.2" "FS" 0
D "4.2.1" "4.3.1" "SF" 0     # el informe no termina antes de que empiece el despliegue
D "4.2.2" "4.3.1" "FS" 0

# Avance real de las primeras tareas
Enviar Patch "/tareas/$($t['1.1.1'].id)" @{ inicioReal = "2026-11-02"; finReal = "2026-11-04"; avance = 100 } | Out-Null
Enviar Patch "/tareas/$($t['1.1.2'].id)" @{ inicioReal = "2026-11-05"; avance = 50 } | Out-Null

# Linea base y luego un cambio: la API pasa de 6 a 9 dias
Enviar Post "/proyectos/$id/linea-base" @{ motivo = "Linea base inicial" } | Out-Null
$r = Enviar Patch "/tareas/$($t['3.1.1'].id)" @{ duracionDias = 9 }

Write-Host ""
Write-Host "Proyecto $codigo creado (id $id)." -ForegroundColor Cyan
Write-Host "Se fijo la linea base y luego 'Implementar la API' paso de 6 a 9 dias; fechas que se movieron:"
foreach ($c in $r.cambios) {
    Write-Host ("  {0} {1}: {2} -> {3}  (antes {4} -> {5})" -f $c.codigo, $c.nombre, $c.inicioDespues, $c.finDespues, $c.inicioAntes, $c.finAntes)
}
Write-Host ""
Write-Host "Abre en el navegador:"
Write-Host "  http://localhost:5173/gantt?id=$id"
Write-Host "  http://localhost:5173/proyectos?id=$id"
Write-Host "Prueba el campo 'Hoy' del Gantt con 2026-11-20 para ver tareas atrasadas."
