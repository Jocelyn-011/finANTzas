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

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val PERCENT = 100f
private const val LABEL_LENGTH = 20

internal val expenseChartColors = listOf(
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
    val database = remember(context) { SecureDatabaseProvider.getDatabase(context) }
    val repository = remember(database) { RoomExpenseRepository(database.expenseDao()) }
    val viewModel = remember(repository, userId) {
        ExpenseViewModel(
            repository = repository,
            userId = userId,
            budgetLimitsFlow = database.budgetLimitDao().observe(userId)
        )
    }
    ExpenseChartsScreen(viewModel = viewModel)
}

@Composable
fun ExpenseChartsScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val roomTotal by viewModel.totalMonthlyClp.collectAsState()
    val dashboardState by viewModel.uiState.collectAsState()
    val categories = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.monthlyImpactClp } }
            .toList().sortedByDescending { it.second }
    }
    val sankeyCategories = remember(expenses) {
        expenses.groupBy { it.category }
            .map { (category, rows) ->
                SankeyCategoryFlow(
                    category = category,
                    totalClp = rows.sumOf { it.monthlyImpactClp.coerceAtLeast(0L) },
                    essentialClp = rows.filterNot { it.isAntExpense }
                        .sumOf { it.monthlyImpactClp.coerceAtLeast(0L) },
                    antExpenseClp = rows.filter { it.isAntExpense }
                        .sumOf { it.monthlyImpactClp.coerceAtLeast(0L) }
                )
            }
            .sortedByDescending { it.totalClp }
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
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Distribución de gastos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        CategoryDonutCard(categories, chartTotal)
        PaymentCards(paymentMethods.toList(), leakageRanking)
        CategoryFlowCard(sankeyCategories)
        ExpenseSummaryCard(
            monthlyTotal = roomTotal,
            antTotal = dashboardState.antMonthlyClp,
            projectedAnnualClp = dashboardState.projectedAnnualClp,
            annualSavingsClp = dashboardState.annualSavingsClp
        )
    }
}

@Composable
private fun CategoryDonutCard(
    categories: List<Pair<String, Long>>,
    chartTotal: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gastos por categoría", style = MaterialTheme.typography.titleMedium)
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
                            drawArc(expenseChartColors[index % expenseChartColors.size], start, sweep, false,
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
                        Box(Modifier.size(10.dp).background(expenseChartColors[index % expenseChartColors.size]))
                        Spacer(Modifier.width(8.dp))
                        Text("${name.toExpenseCategoryLabel()}: ${clp(amount)}", style = MaterialTheme.typography.bodySmall)
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
    ChartCard("Gastos por medio de pago") {
        if (paymentMethods.isNotEmpty()) {
            DonutChart(
                amounts = paymentMethods.map { it.second },
                labels = paymentMethods.map { it.first.displayName },
                colors = expenseChartColors
            )
        } else Text("No hay gastos registrados.")
    }

    ChartCard("Gasto hormiga por medio de pago") {
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
private fun CategoryFlowCard(categories: List<SankeyCategoryFlow>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Distribución por categoría y tipo de gasto", style = MaterialTheme.typography.titleMedium)
            Text(
                "Sigue cómo se reparte tu gasto mensual.",
                style = MaterialTheme.typography.bodySmall
            )
            if (categories.any { it.totalClp > 0L }) {
                SankeyDiagramCanvas(categories = categories)
            } else {
                Text("No hay datos para representar.")
            }
        }
    }
}

@Composable
private fun ExpenseSummaryCard(
    monthlyTotal: Long,
    antTotal: Long,
    projectedAnnualClp: Long,
    annualSavingsClp: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen de gastos", style = MaterialTheme.typography.titleMedium)
            SummaryMetric("Total mensual", monthlyTotal)
            SummaryMetric("Proyección anual", projectedAnnualClp)
            SummaryMetric("Gasto hormiga al mes", antTotal)
            SummaryMetric("Ahorro potencial al año", annualSavingsClp)
            Text(
                "Estimación de ahorro si reduces los gastos hormiga.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, amount: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(clp(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
