$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root = (Get-Location).Path
$src = Join-Path $root 'app\src\main\java\cl\finantzas\app'
$nav = Join-Path $src 'ui\navigation\FinANTzasNavigation.kt'
$charts = Join-Path $src 'ui\components\ExpenseChartsScreen.kt'
$gradle = Join-Path $root 'gradlew.bat'
if (!(Test-Path $nav) -or !(Test-Path $gradle)) { throw 'Ejecuta este script desde la raiz de finANTzas.' }
if (Test-Path $charts) { throw "Ya existe $charts. No se sobrescribira." }
$original = [IO.File]::ReadAllText($nav)
$target = 'PantallaTemporal("Sankey, Anillos y % de Fuga")'
if (!$original.Contains($target)) { throw 'No coincide la ruta provisional de graficos. No se modifico nada.' }
$backup = "$nav.bak-graficos"
if (Test-Path $backup) { throw "Ya existe respaldo $backup. Revisalo antes de continuar." }
Copy-Item $nav $backup
$created = $false
try {
    $code = @'
package cl.finantzas.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale

private const val CHART_DEMO_USER = "finantzas-demo-local"
private val chartColors = listOf(
    Color(0xFF008577), Color(0xFFEF8C35), Color(0xFF5D68C4),
    Color(0xFFE15F73), Color(0xFF7B8A8B), Color(0xFF6D9F4A)
)
private fun clp(value: Long): String = "$" + NumberFormat.getIntegerInstance(Locale("es", "CL")).format(value)

@Composable
fun ExpenseChartsScreen() {
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val viewModel = remember(repository) { ExpenseViewModel(repository, CHART_DEMO_USER) }
    val expenses by viewModel.expenses.collectAsState()
    val roomTotal by viewModel.totalMonthlyClp.collectAsState()
    val categories = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.monthlyImpactClp } }
            .toList().sortedByDescending { it.second }
    }
    val chartTotal = categories.sumOf { it.second }
    val antTotal = expenses.filter { it.isAntExpense }.sumOf { it.monthlyImpactClp }
    val consistent = chartTotal == roomTotal

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Distribucion de gastos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Datos reactivos desde Room (CLP)", style = MaterialTheme.typography.bodySmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Anillo por categoria", style = MaterialTheme.typography.titleMedium)
                if (chartTotal > 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            val stroke = 24.dp.toPx()
                            var start = -90f
                            categories.forEachIndexed { index, pair ->
                                val sweep = 360f * pair.second.toFloat() / chartTotal.toFloat()
                                drawArc(chartColors[index % chartColors.size], start, sweep, false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Butt))
                                start += sweep
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(clp(chartTotal), fontWeight = FontWeight.Bold)
                            Text("Total mensual", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    categories.forEachIndexed { index, (name, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(chartColors[index % chartColors.size]))
                            Spacer(Modifier.width(8.dp))
                            Text("$name: ${clp(amount)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else Text("No hay gastos registrados.")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Flujo de gastos por categoria", style = MaterialTheme.typography.titleMedium)
                Text("Vista proporcional tipo Sankey: total mensual hacia categorias", style = MaterialTheme.typography.bodySmall)
                if (chartTotal > 0L) {
                    Box(Modifier.fillMaxWidth().height(24.dp).background(MaterialTheme.colorScheme.primary))
                    Text("Total: ${clp(chartTotal)}", style = MaterialTheme.typography.labelMedium)
                    categories.forEachIndexed { index, (name, amount) ->
                        val fraction = (amount.toDouble() / chartTotal.toDouble()).toFloat().coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("$name - ${clp(amount)} (${String.format(Locale.US, "%.1f", fraction * 100f)}%)",
                                style = MaterialTheme.typography.bodySmall)
                            Box(Modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(Modifier.fillMaxWidth(fraction).height(16.dp)
                                    .background(chartColors[index % chartColors.size]))
                            }
                        }
                    }
                } else Text("No hay datos para representar.")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Conciliacion Room / Graficos / KPIs", style = MaterialTheme.typography.titleMedium)
                Text("Total Room (DAO): ${clp(roomTotal)}")
                Text("Suma por categorias: ${clp(chartTotal)}")
                Text("Proyeccion anual: ${clp(roomTotal * 12L)}")
                Text("Gasto hormiga mensual: ${clp(antTotal)}")
                Text("Ahorro potencial anual: ${clp(antTotal * 12L)}")
                Text(if (consistent) "CONCILIACION OK: montos coinciden" else "REVISAR: montos no coinciden",
                    color = if (consistent) Color(0xFF16784C) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold)
                Text("Los datos se actualizan al modificar Room.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
'@
    [IO.File]::WriteAllText($charts, $code, [Text.UTF8Encoding]::new($false))
    $created = $true
    $updated = $original.Replace($target, 'ExpenseChartsScreen()')
    $updated = $updated.Replace('import cl.finantzas.app.ui.components.ExpenseDemoScreen', 'import cl.finantzas.app.ui.components.ExpenseDemoScreen' + "`n" + 'import cl.finantzas.app.ui.components.ExpenseChartsScreen')
    [IO.File]::WriteAllText($nav, $updated, [Text.UTF8Encoding]::new($false))
    Write-Host 'Graficos agregados. Compilando...' -ForegroundColor Cyan
    & $gradle ':app:assembleDebug' '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw "Gradle devolvio codigo $LASTEXITCODE" }
    Write-Host 'BUILD OK. Abre la pestana Graficos y comprueba conciliacion.' -ForegroundColor Green
    Write-Host 'Archivos modificados:'
    git status --short
    Write-Host "Respaldo temporal: $backup (no incluir en Git)"
} catch {
    Write-Warning "Error: $_"
    Copy-Item $backup $nav -Force
    if ($created -and (Test-Path $charts)) { Remove-Item $charts -Force }
    Write-Warning 'Se revirtieron los cambios de este script. Tus otros archivos no se tocaron.'
    throw
}
