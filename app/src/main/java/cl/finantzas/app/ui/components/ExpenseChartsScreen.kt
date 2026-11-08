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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import cl.aiep.finantzas.domain.rules.PaymentInstrument
import cl.aiep.finantzas.domain.rules.PaymentMethodLeakageResult
import java.text.NumberFormat
import java.util.Locale

private const val CHART_DEMO_USER = "finantzas-demo-local"
private val chartColors = listOf(
    Color(0xFF008577), Color(0xFFEF8C35), Color(0xFF5D68C4),
    Color(0xFFE15F73), Color(0xFF7B8A8B), Color(0xFF6D9F4A)
)

private fun clp(value: Long): String = "$" + NumberFormat.getIntegerInstance(Locale("es", "CL")).format(value)

private fun instrumentFor(paymentMethod: String): PaymentInstrument = when (paymentMethod.trim().uppercase()) {
    "CREDIT_CARD", "CREDIT" -> PaymentInstrument.CreditCard
    "DEBIT", "DEBIT_CARD" -> PaymentInstrument.DebitCard
    "CASH" -> PaymentInstrument.Cash
    "NFC_MOBILE_WALLET", "NFC" -> PaymentInstrument.NfcMobileWallet
    "BANK_TRANSFER", "TRANSFER" -> PaymentInstrument.BankTransfer
    else -> PaymentInstrument.fromCode(paymentMethod)
}

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
            .mapValues { (_, rows) -> rows.sumOf { it.monthlyImpactClp } }
            .toList().sortedByDescending { it.second }
    }
    val paymentMethods = remember(expenses) {
        expenses.groupBy { instrumentFor(it.paymentMethod) }
            .mapValues { (_, rows) -> rows.sumOf { it.monthlyImpactClp } }
            .toList().sortedByDescending { it.second }
    }
    val chartTotal = categories.sumOf { it.second }
    val antTotal = expenses.filter { it.isAntExpense }.sumOf { it.monthlyImpactClp }
    val leakageRanking = paymentMethods.map { (instrument, total) ->
        val ant = expenses.asSequence()
            .filter { instrumentFor(it.paymentMethod) == instrument && it.isAntExpense }
            .sumOf { it.monthlyImpactClp }
        PaymentMethodLeakageResult(
            instrument = instrument,
            totalExpenseAmountClp = total,
            antExpenseAmountClp = ant,
            leakagePercentage = if (total > 0L) ((ant * 100L) / total).toInt() else 0
        )
    }.sortedByDescending { it.leakagePercentage }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Distribución de gastos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Datos reactivos desde Room (CLP)", style = MaterialTheme.typography.bodySmall)

        ChartCard("Anillo por categoría") {
            if (chartTotal > 0L) {
                DonutChart(
                    amounts = categories.map { it.second },
                    labels = categories.map { it.first },
                    colors = chartColors
                )
                categories.forEachIndexed { index, (name, amount) ->
                    LegendRow(chartColors[index % chartColors.size], "$name: ${clp(amount)}")
                }
            } else Text("No hay gastos registrados.")
        }

        ChartCard("Anillo por medio de pago") {
            if (paymentMethods.isNotEmpty()) {
                DonutChart(
                    amounts = paymentMethods.map { it.second },
                    labels = paymentMethods.map { it.first.displayName },
                    colors = chartColors
                )
            } else Text("No hay gastos registrados.")
        }

        ChartCard("Fuga por medio de pago (RN-04)") {
            if (leakageRanking.isEmpty()) Text("No hay datos para representar.")
            leakageRanking.forEach { row ->
                Text(
                    "${row.instrument.displayName}: ${row.leakagePercentage}% · " +
                        "${clp(row.antExpenseAmountClp)} de ${clp(row.totalExpenseAmountClp)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        ChartCard("Conciliación y KPIs") {
            Text("Total Room (DAO): ${clp(roomTotal)}")
            Text("Suma por categorías: ${clp(chartTotal)}")
            Text("Proyección anual: ${clp(roomTotal * 12L)}")
            Text("Gasto hormiga mensual: ${clp(antTotal)}")
            Text("Ahorro potencial anual: ${clp(antTotal * 12L)}")
            val consistent = chartTotal == roomTotal
            Text(
                if (consistent) "CONCILIACIÓN OK" else "REVISAR: montos no coinciden",
                color = if (consistent) Color(0xFF16784C) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DonutChart(amounts: List<Long>, labels: List<String>, colors: List<Color>) {
    val total = amounts.sumOf { it }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size(132.dp)) {
            var start = -90f
            amounts.forEachIndexed { index, amount ->
                val sweep = if (total > 0L) 360f * amount.toFloat() / total.toFloat() else 0f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(clp(total), fontWeight = FontWeight.Bold)
            Text("Mensual", style = MaterialTheme.typography.bodySmall)
            labels.forEachIndexed { index, label ->
                Text("${label.take(20)} · ${amounts[index].toFloat().div(total.coerceAtLeast(1L)).times(100).toInt()}%")
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
