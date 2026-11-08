package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.finantzas.app.ui.viewmodel.BudgetViewModel
import java.util.Locale

@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val cards by viewModel.cards.collectAsState()
    val status by viewModel.status.collectAsState()
    val format = remember { cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Alertas y Presupuestos", style = MaterialTheme.typography.titleLarge)
        Text("Impacto mensual de los gastos registrados · CLP")
        status?.let { Text(it) }
        cards.forEach { card ->
            var input by remember(card.category, card.limit) { mutableStateOf(card.limit.toString()) }
            val color = when { card.percentage >= cl.aiep.finantzas.domain.rules.FinancialThresholds.BUDGET_CRITICAL_PERCENTAGE -> MaterialTheme.colorScheme.error
                card.percentage >= cl.aiep.finantzas.domain.rules.FinancialThresholds.BUDGET_WARNING_PERCENTAGE -> Color(0xFF9C6500)
                else -> Color(0xFF16784C) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium)
                    Text("Gasto: ${format.formatClp(card.spent)} / Límite: ${format.formatClp(card.limit)} CLP")
                    Text(String.format(Locale("es", "CL"), "%.1f%% consumido", card.percentage), color = color)
                    LinearProgressIndicator(progress = { card.fraction }, modifier = Modifier.fillMaxWidth(), color = color)
                    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Límite mensual CLP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = { viewModel.save(card.category, input) }) { Text("Guardar") }
                }
            }
        }
    }
}
