# Diagnostico de subtarea 2: no modifica codigo ni datos
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$base = Join-Path $root 'app\src\main\java\cl\finantzas\app'
$report = Join-Path $root 'REPORTE_VALIDACION_CLP.txt'
$log = New-Object System.Collections.Generic.List[string]
function Add-Line([string]$message) { $log.Add($message); Write-Host $message }
function Read-Source([string]$relative) {
    $path = Join-Path $base $relative
    if (Test-Path $path) { return [System.IO.File]::ReadAllText($path) }
    return ''
}
function Check([string]$name, [bool]$condition) {
    if ($condition) { Add-Line ('[OK] ' + $name) } else { Add-Line ('[REVISAR] ' + $name) }
}
try {
    if (-not (Test-Path (Join-Path $root 'gradlew.bat'))) { throw 'Ejecuta desde la carpeta raiz finANTzas.' }
    Add-Line '=== VALIDACION FINANTZAS / CLICKUP SUBTAREA 2 ==='
    Add-Line ('Fecha: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    Add-Line ('Rama: ' + (& git branch --show-current))
    $status = @(& git status --short)
    $codeChanges = @($status | Where-Object { $_ -notmatch 'REPORTE_VALIDACION_CLP\.txt|validar_finantzas_clp\.ps1|finantzas_restaurar_demo\.ps1' })
    Add-Line ('Cambios en codigo Git: ' + $codeChanges.Count)
    foreach ($line in $codeChanges) { Add-Line ('  ' + $line) }

    $demo = Read-Source 'data\demo\ExpenseDemoDataClp.kt'
    $dao = Read-Source 'data\local\dao\ExpenseDao.kt'
    $screen = Read-Source 'ui\components\ExpenseDemoScreen.kt'
    $nav = Read-Source 'ui\navigation\FinANTzasNavigation.kt'
    $vm = Read-Source 'ui\viewmodel\ExpenseViewModel.kt'
    $database = Read-Source 'data\local\database\SecureDatabaseProvider.kt'
    Add-Line '=== COMPROBACIONES ESTATICAS ==='
    Check 'Datos demo: Cafe 3500' ([bool]($demo -match '3500L?\b'))
    Check 'Datos demo: Streaming 26900' ([bool]($demo -match '26900L?\b'))
    Check 'Datos demo: Arriendo 380000' ([bool]($demo -match '380000L?\b'))
    Check 'Datos demo: Bip 42000' ([bool]($demo -match '42000L?\b'))
    Check 'Boton restaurar presente' ([bool]($screen -match 'Restaurar datos de ejemplo'))
    Check 'DAO consulta gastos por usuario' ([bool]($dao -match 'getExpensesByUser'))
    Check 'DAO consulta total mensual' ([bool]($dao -match 'getTotalMonthlyImpactClp'))
    Check 'ViewModel usa StateFlow' ([bool]($vm -match 'stateIn\('))
    Check 'Persistencia cifrada SQLCipher' ([bool]($database -match 'SupportFactory|sqlcipher|SQLCipher'))
    $uiFiles = @(Get-ChildItem (Join-Path $base 'ui') -Recurse -Filter '*.kt' -ErrorAction SilentlyContinue)
    $chartFiles = @($uiFiles | Where-Object { $_.Name -match 'Sankey|Donut|Ring|Anillo|Chart|Grafico' })
    Add-Line ('Archivos UI relacionados con graficos: ' + $chartFiles.Count)
    foreach ($f in $chartFiles) { Add-Line ('  ' + $f.FullName.Substring($root.Length + 1)) }
    $isPlaceholder = $nav -match 'PantallaTemporal\("Sankey'
    Check 'Ruta de graficos implementada (no temporal)' (-not $isPlaceholder)
    Add-Line '=== MONTOS ESPERADOS (4 IMPORTES MENSUALES) ==='
    $items = [ordered]@{ Cafe=3500L; Streaming=26900L; Arriendo=380000L; Bip=42000L }
    foreach ($item in $items.GetEnumerator()) { Add-Line ('  ' + $item.Key + ': $' + $item.Value) }
    $total = [long](($items.Values | Measure-Object -Sum).Sum)
    Add-Line ('  TOTAL MENSUAL: $' + $total)
    Add-Line ('  PROYECCION ANUAL: $' + ($total * 12L))
    Add-Line '=== COMPILACION ==='
    & .\gradlew.bat :app:assembleDebug --console=plain 2>&1 | ForEach-Object {
        $s = [string]$_
        if ($s -match 'BUILD SUCCESSFUL|BUILD FAILED|^e: |^FAILURE:|^\* What went wrong:') { Add-Line $s }
    }
    $buildExit = $LASTEXITCODE
    Check 'Gradle assembleDebug' ($buildExit -eq 0)
    Add-Line '=== LIMITACIONES DE ESTA PRUEBA ==='
    Add-Line 'Este diagnostico no lee filas de la BD cifrada ni confirma graficos en pantalla.'
    Add-Line 'Para cerrar ClickUp: comprobar datos en app/emulador y visualizaciones reales.'
    Add-Line 'No incluye claves, valores de local.properties ni datos privados.'
} catch {
    Add-Line ('[ERROR] ' + $_.Exception.Message)
} finally {
    [System.IO.File]::WriteAllLines($report, [string[]]$log, [System.Text.UTF8Encoding]::new($false))
    Write-Host ('Reporte: ' + $report)
}
