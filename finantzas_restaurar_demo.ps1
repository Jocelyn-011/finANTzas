$ErrorActionPreference = 'Stop'
$base = 'app/src/main/java/cl/finantzas/app'
$demo = "$base/data/demo/ExpenseDemoDataClp.kt"
$dao = "$base/data/local/dao/ExpenseDao.kt"
$screen = "$base/ui/components/ExpenseDemoScreen.kt"
foreach ($f in @($demo,$dao,$screen)) { if (!(Test-Path $f)) { throw "Archivo no encontrado: $f" } }
$branch = (git branch --show-current).Trim()
if ($branch -ne 'feature/optimizacion-flow-datos-clp') { throw "Rama inesperada: $branch" }
# Validacion Git omitida: estado limpio comprobado previamente.
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Save-Utf8($path,$value) { [System.IO.File]::WriteAllText((Resolve-Path $path).Path,$value,$utf8) }
$demoText = @'
package cl.finantzas.app.data.demo

import cl.finantzas.app.data.local.entity.ExpenseEntityClp

object ExpenseDemoDataClp {
    fun create(userId: String): List<ExpenseEntityClp> {
        val now = System.currentTimeMillis()

        fun expense(concept: String, monthlyAmount: Long, category: String, isAnt: Boolean) =
            ExpenseEntityClp(
                id = "demo-" + concept.lowercase().replace(" ", "-"),
                userId = userId,
                concept = concept,
                unitAmountClp = monthlyAmount,
                monthlyImpactClp = monthlyAmount,
                weeklyFrequency = 0,
                expenseType = "FIXED",
                category = category,
                paymentMethod = "DEBIT",
                isAntExpense = isAnt,
                triggeredRuleCode = "",
                ruleReason = "",
                createdAt = now
            )

        // Los valores de ClickUp se interpretan como montos mensuales de demostracion.
        return listOf(
            expense("Cafe", 3500L, "Alimentacion", true),
            expense("Streaming", 26900L, "Entretenimiento", false),
            expense("Arriendo", 380000L, "Vivienda", false),
            expense("Bip", 42000L, "Transporte", false)
        )
    }
}
'@
$daoText = Get-Content $dao -Raw -Encoding UTF8
$anchor = '    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY created_at ASC")'
if (!$daoText.Contains($anchor)) { throw 'No se encontro punto seguro de insercion en ExpenseDao.kt' }
if ($daoText.Contains('restoreDemoExpenses')) { throw 'La funcion de restauracion ya existe.' }
$extra = @'
    @Query("DELETE FROM expenses WHERE user_id = :userId AND id LIKE 'demo-%'")
    suspend fun deleteDemoExpenses(userId: String)

    @Transaction
    suspend fun restoreDemoExpenses(userId: String, demoExpenses: List<ExpenseEntityClp>) {
        deleteDemoExpenses(userId)
        demoExpenses.forEach { insert(it) }
    }

'@
$daoText = $daoText.Replace($anchor, $extra + $anchor)
$screenText = @'
package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.demo.ExpenseDemoDataClp
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

private const val DEMO_USER_ID = "finantzas-demo-local"

@Composable
fun ExpenseDemoScreen() {
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val viewModel = remember(repository) { ExpenseViewModel(repository, DEMO_USER_ID) }
    val expenses by viewModel.expenses.collectAsState()
    val totalMonthly by viewModel.totalMonthlyClp.collectAsState()
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val antMonthly = expenses.filter { it.isAntExpense }.sumOf { it.monthlyImpactClp }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        KpiCards(
            gastoMensual = totalMonthly,
            proyeccionAnual = totalMonthly * 12L,
            gastoHormiga = antMonthly,
            ahorroPotencial = antMonthly * 12L
        )
        Button(
            onClick = {
                if (!restoring) {
                    restoring = true
                    error = null
                    scope.launch {
                        try {
                            dao.restoreDemoExpenses(DEMO_USER_ID, ExpenseDemoDataClp.create(DEMO_USER_ID))
                        } catch (e: Exception) {
                            error = "No se pudieron restaurar los datos: ${e.message ?: "error desconocido"}"
                        } finally {
                            restoring = false
                        }
                    }
                }
            },
            enabled = !restoring,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (restoring) "Restaurando..." else "Restaurar datos de ejemplo") }
        if (error != null) Text(error!!)
    }
}
'@
Save-Utf8 $demo $demoText
Save-Utf8 $dao $daoText
Save-Utf8 $screen $screenText
Write-Host 'Archivos actualizados. Compilando...' -ForegroundColor Cyan
& .\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo; no se ha realizado commit.' }
git diff --check
if ($LASTEXITCODE -ne 0) { throw 'git diff --check fallo.' }
Write-Host 'BUILD OK. Revisa la app antes de hacer commit.' -ForegroundColor Green
git status -sb
