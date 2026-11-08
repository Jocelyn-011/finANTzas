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
import cl.aiep.finantzas.domain.rules.PaymentInstrument
import cl.aiep.finantzas.domain.rules.FinancialAnalyticsCalculator
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import java.util.Locale

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val PERCENT = 100f
private const val MONTHS_PER_YEAR = 12L
private const val LABEL_LENGTH = 20

private val consistentColor = Color(0xFF16784C)

private val chartColors = listOf(
    Color(0xFF008577), Color(0xFFEF8C35), Color(0xFF5D68C4),
    Color(0xFFE15F73), Color(0xFF7B8A8B), Color(0xFF6D9F4A)
)
private fun clp(value: Long): String = ClpCurrencyFormatter.formatClp(value)

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
    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val viewModel = remember(repository, userId) { ExpenseViewModel(repository, userId) }
    val expenses by viewModel.expenses.collectAsState()
    val roomTotal by viewModel.totalMonthlyClp.collectAsState()
    val categories = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.monthlyImpactClp } }
            .toList().sortedByDescending { it.second }
    }
    val paymentMethods = remember(expenses) {
        expenses.groupBy { instrumentFor(it.paymentMethod) }
            .mapValues { (_, rows) -> rows.sumOf { it.monthlyImpactClp } }
    }
    val leakageRanking = remember(expenses) {
        paymentMethods.map { (instrument, total) ->
            val ant = expenses.filter { instrumentFor(it.paymentMethod) == instrument && it.isAntExpense }
                .sumOf { it.monthlyImpactClp }
            FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(instrument, total, ant)
        }.sortedByDescending { it.leakagePercentage }
    }
    val chartTotal = categories.sumOf { it.second }
    val antTotal = expenses.filter { it.isAntExpense }.sumOf { it.monthlyImpactClp }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Distribucion de gastos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Datos reactivos desde Room (CLP)", style = MaterialTheme.typography.bodySmall)
        CategoryDonutCard(categories, chartTotal)
        PaymentCards(paymentMethods.toList(), leakageRanking)
        CategoryFlowCard(categories, chartTotal)
        ReconciliationCard(roomTotal, chartTotal, antTotal)
    }
}

@Composable
private fun CategoryDonutCard(
    categories: List<Pair<String, Long>>,
    chartTotal: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Anillo por categoria", style = MaterialTheme.typography.titleMedium)
            if (chartTotal > 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        val stroke = 24.dp.toPx()
                        var start = START_ANGLE
                        categories.forEachIndexed { index, pair ->
                            val sweep = FULL_CIRCLE * pair.second.toFloat() / chartTotal.toFloat()
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
}

@Composable
private fun PaymentCards(
    paymentMethods: List<Pair<PaymentInstrument, Long>>,
    leakageRanking: List<cl.aiep.finantzas.domain.rules.PaymentMethodLeakageResult>
) {
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
}

@Composable
private fun CategoryFlowCard(
    categories: List<Pair<String, Long>>,
    chartTotal: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Flujo de gastos por categoria", style = MaterialTheme.typography.titleMedium)
            Text(
            "Vista proporcional tipo Sankey: total mensual hacia categorias",
            style = MaterialTheme.typography.bodySmall
        )
            if (chartTotal > 0L) {
                Box(Modifier.fillMaxWidth().height(24.dp).background(MaterialTheme.colorScheme.primary))
                Text("Total: ${clp(chartTotal)}", style = MaterialTheme.typography.labelMedium)
                categories.forEachIndexed { index, (name, amount) ->
                    val fraction = (amount.toDouble() / chartTotal.toDouble()).toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("$name - ${clp(amount)} (${String.format(Locale.US, "%.1f", fraction * PERCENT)}%)",
                            style = MaterialTheme.typography.bodySmall)
                        Box(
                        Modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                            Box(Modifier.fillMaxWidth(fraction).height(16.dp)
                                .background(chartColors[index % chartColors.size]))
                        }
                    }
                }
            } else Text("No hay datos para representar.")
        }
    }

}

@Composable
private fun ReconciliationCard(
    roomTotal: Long,
    chartTotal: Long,
    antTotal: Long
) {
    val consistent = chartTotal == roomTotal
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Conciliacion Room / Graficos / KPIs", style = MaterialTheme.typography.titleMedium)
            Text("Total Room (DAO): ${clp(roomTotal)}")
            Text("Suma por categorias: ${clp(chartTotal)}")
            Text("Proyeccion anual: ${clp(roomTotal * MONTHS_PER_YEAR)}")
            Text("Gasto hormiga mensual: ${clp(antTotal)}")
            Text("Ahorro potencial anual: ${clp(antTotal * MONTHS_PER_YEAR)}")
            Text(if (consistent) "CONCILIACION OK: montos coinciden" else "REVISAR: montos no coinciden",
                color = if (consistent) consistentColor else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold)
            Text("Los datos se actualizan al modificar Room.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DonutChart(amounts: List<Long>, labels: List<String>, colors: List<Color>) {
    val total = amounts.sumOf { it }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size(132.dp)) {
            var start = START_ANGLE
            amounts.forEachIndexed { index, amount ->
                val sweep = if (total > 0L) FULL_CIRCLE * amount.toFloat() / total.toFloat() else 0f
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
                val percentage = amounts[index].toFloat().div(total.coerceAtLeast(1L)).times(PERCENT).toInt()
                Text("${label.take(LABEL_LENGTH)} \u00b7 $percentage%")
            }
        }
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
