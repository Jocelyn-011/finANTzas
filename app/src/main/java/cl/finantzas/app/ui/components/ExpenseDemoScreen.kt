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
import kotlinx.coroutines.CancellationException

@Composable
fun ExpenseDemoScreen() {
    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val viewModel = remember(repository, userId) { ExpenseViewModel(repository, userId) }
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
        MonthlyExpenseEntry(repository, userId)
        Button(
            onClick = {
                if (!restoring) {
                    restoring = true
                    error = null
                    scope.launch {
                        try {
                            dao.restoreDemoExpenses(userId, ExpenseDemoDataClp.create(userId))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            error = "No se pudieron restaurar los datos de ejemplo."
                        } finally {
                            restoring = false
                        }
                    }
                }
            },
            enabled = !restoring && userId == LOCAL_EXPENSE_USER,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (restoring) "Restaurando..." else "Restaurar datos de ejemplo") }
        error?.let { Text(it) }
    }
}
