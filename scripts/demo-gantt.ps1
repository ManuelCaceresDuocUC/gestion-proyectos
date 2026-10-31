# Crea por la API un proyecto de ejemplo para probar el Gantt: 3 etapas, actividades y subactividades,
# los 4 tipos de dependencia, un hito, avances, linea base y luego un cambio de duracion que genera desviaciones.
# Requisitos: base arriba y backend corriendo en http://localhost:8080 (docker compose up --build -d, o mvn spring-boot:run).
# Ejecutar desde la raiz del repo:  powershell -ExecutionPolicy Bypass -File .\scripts\demo-gantt.ps1
$ErrorActionPreference = "Stop"
$api = "http://localhost:8080/api"

function Enviar($metodo, $ruta, $obj) {
    $json = $obj | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Method $metodo -Uri "$api$ruta" -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($json))
}

$codigo = "GANTT-" + (Get-Date -Format "HHmmss")
$p = Enviar Post "/proyectos" @{ codigo = $codigo; nombre = "Proyecto de ejemplo para el Gantt"; presupuesto = 5000000; moneda = "CLP"; fechaInicio = "2026-11-02" }
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

# EDT: etapa > actividad > subactividad (las dependencias van entre tareas sin hijos)
$e1 = Nueva $null "Planificacion" $null
$a11 = Nueva $e1 "Levantamiento" $null
$s111 = Nueva $a11 "Entrevistas" 3
$s112 = Nueva $a11 "Informe de requisitos" 2
$a12 = Nueva $e1 "Diseno" $null
$s121 = Nueva $a12 "Arquitectura" 4
$s122 = Nueva $a12 "Prototipo" 3

$e2 = Nueva $null "Construccion" $null
$a21 = Nueva $e2 "Backend" $null
$s211 = Nueva $a21 "API" 6
$s212 = Nueva $a21 "Base de datos" 3
$a22 = Nueva $e2 "Frontend" $null
$s221 = Nueva $a22 "Pantallas" 5

$e3 = Nueva $null "Cierre" $null
$a31 = Nueva $e3 "Pruebas" 4
$a32 = Nueva $e3 "Entrega" 0

# Dependencias de los 4 tipos
Dep $s111 $s112 "FS" 0   # informe empieza al terminar las entrevistas
Dep $s112 $s121 "FS" 0
Dep $s121 $s122 "SS" 1   # prototipo empieza 1 dia despues de la arquitectura
Dep $s111 $s122 "SF" 0   # fin del prototipo no antes del inicio de las entrevistas
Dep $s122 $s211 "FS" 0
Dep $s211 $s212 "FF" 0   # BD termina junto con la API
Dep $s211 $s221 "FS" 1
Dep $s212 $a31 "FS" 0
Dep $s221 $a31 "FS" 0
Dep $a31 $a32 "FS" 0

# Avance real de las primeras tareas
Enviar Patch "/tareas/$($s111.id)" @{ inicioReal = "2026-11-02"; finReal = "2026-11-04"; avance = 100 } | Out-Null
Enviar Patch "/tareas/$($s112.id)" @{ inicioReal = "2026-11-05"; avance = 50 } | Out-Null

# Linea base y luego un cambio: la API pasa de 6 a 9 dias
Enviar Post "/proyectos/$id/linea-base" @{ motivo = "Linea base inicial" } | Out-Null
$r = Enviar Patch "/tareas/$($s211.id)" @{ duracionDias = 9 }

Write-Host ""
Write-Host "Proyecto $codigo creado (id $id)." -ForegroundColor Cyan
Write-Host "Se fijo la linea base y luego la API paso de 6 a 9 dias; fechas que se movieron:"
foreach ($c in $r.cambios) {
    Write-Host ("  {0} {1}: {2} -> {3}  (antes {4} -> {5})" -f $c.codigo, $c.nombre, $c.inicioDespues, $c.finDespues, $c.inicioAntes, $c.finAntes)
}
Write-Host ""
Write-Host "Abre en el navegador:"
Write-Host "  http://localhost:5173/gantt?id=$id"
Write-Host "  http://localhost:5173/proyectos?id=$id"
Write-Host "Prueba el campo 'Hoy' del Gantt con 2026-11-20 para ver tareas atrasadas."
