package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val repository = remember(context) {
        RoomExpenseRepository(SecureDatabaseProvider.getDatabase(context).expenseDao())
    }
    val viewModel = remember(repository) { ExpenseViewModel(repository, DEMO_USER_ID) }
    val expenses by viewModel.expenses.collectAsState()
    val totalMonthly by viewModel.totalMonthlyClp.collectAsState()
    val scope = rememberCoroutineScope()
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
                scope.launch {
                    ExpenseDemoDataClp.create(DEMO_USER_ID).forEach { repository.insert(it) }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cargar datos de demostración") }
    }
}