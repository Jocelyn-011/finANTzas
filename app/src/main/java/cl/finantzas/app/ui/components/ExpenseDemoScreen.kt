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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ExpenseDemoScreen() {
    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val database = remember(context) { SecureDatabaseProvider.getDatabase(context) }
    val repository = remember(database) { RoomExpenseRepository(database.expenseDao()) }
    val viewModel = remember(repository, userId) {
        ExpenseViewModel(
            repository = repository,
            userId = userId,
            budgetLimitsFlow = database.budgetLimitDao().observe(userId)
        )
    }
    ExpenseDemoScreen(viewModel = viewModel)
}

@Composable
fun ExpenseDemoScreen(viewModel: ExpenseViewModel) {
    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val dashboardState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        KpiCards(uiState = dashboardState)
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
