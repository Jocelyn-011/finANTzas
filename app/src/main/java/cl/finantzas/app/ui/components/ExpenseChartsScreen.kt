package cl.finantzas.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.FinancialAnalyticsCalculator
import cl.aiep.finantzas.domain.rules.PaymentInstrument
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.ui.viewmodel.BudgetViewModel
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f

internal val expenseChartColors = listOf(
    Color(0xFF38A3A5), // Turquesa Petróleo
    Color(0xFF2EC4B6), // Verde Esmeralda
    Color(0xFFF46036), // Coral Hormiga
    Color(0xFFF6AA1C), // Ámbar
    Color(0xFF7DD3FC)  // Celeste
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
fun ExpenseChartsScreen(
    viewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel? = null
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val expenses by viewModel.expenses.collectAsState()
    val roomTotal by viewModel.totalMonthlyClp.collectAsState()
    val dashboardState by viewModel.uiState.collectAsState()

    val categories = remember(expenses) {
        val grouped = expenses.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.monthlyImpactClp.coerceAtLeast(0L) } }
            .toList()
            .sortedByDescending { it.second }
        grouped.ifEmpty {
            listOf(
                "FOOD_AND_GROCERIES" to 195_000L,
                "MICRO_PURCHASES_AND_SNACKS" to 92_000L,
                "ENTERTAINMENT_SUBSCRIPTIONS" to 68_000L
            )
        }
    }

    val sankeyCategories = remember(expenses) {
        val list = expenses.groupBy { it.category }
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
        list.ifEmpty {
            listOf(
                SankeyCategoryFlow("FOOD_AND_GROCERIES", 195_000L, 195_000L, 0L),
                SankeyCategoryFlow("MICRO_PURCHASES_AND_SNACKS", 92_000L, 22_000L, 70_000L),
                SankeyCategoryFlow("ENTERTAINMENT_SUBSCRIPTIONS", 68_000L, 41_100L, 26_900L)
            )
        }
    }

    val paymentMethods = remember(expenses) {
        val grouped = expenses.groupBy { instrumentFor(it.paymentMethod) }
            .mapValues { (_, rows) -> rows.sumOf { it.monthlyImpactClp.coerceAtLeast(0L) } }
            .toList()
            .sortedByDescending { it.second }
        grouped.ifEmpty {
            listOf(
                PaymentInstrument.DebitCard to 195_000L,
                PaymentInstrument.CreditCard to 92_000L,
                PaymentInstrument.NfcMobileWallet to 68_000L
            )
        }
    }

    val leakageRanking = remember(expenses, paymentMethods) {
        paymentMethods.map { (instrument, total) ->
            val ant = expenses.filter { instrumentFor(it.paymentMethod) == instrument && it.isAntExpense }
                .sumOf { it.monthlyImpactClp.coerceAtLeast(0L) }
            FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(instrument, total, ant)
        }.sortedByDescending { it.leakagePercentage }
    }

    val monthlyTotalDisplay = if (roomTotal > 0L) roomTotal else categories.sumOf { it.second }
    val annualSavingsDisplay = if (dashboardState.annualSavingsClp > 0L) dashboardState.annualSavingsClp else 420_000L

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resumen de Gastos",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = "Tendencia gastos",
                            tint = CoralHormiga,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${clp(monthlyTotalDisplay)} CLP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Text(
                        text = "Total mensual CLP",
                        fontSize = 11.sp,
                        color = CoralHormiga
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ahorro Anual Proyectado",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = "Tendencia ahorro",
                            tint = MintBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${clp(annualSavingsDisplay)} CLP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintBright
                    )
                    Text(
                        text = "Reduciendo gasto hormiga",
                        fontSize = 11.sp,
                        color = MintBright
                    )
                }
            }
        }

        SavingsSimulatorScreen(viewModel = viewModel, embedded = true)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Gastos por Categoría y Medios de Pago",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniRingColumn(
                        title = "Categorías",
                        items = categories.map { it.first.toExpenseCategoryLabel().take(12) to it.second },
                        textColor = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    MiniRingColumn(
                        title = "Medios de Pago",
                        items = paymentMethods.map { it.first.displayName.take(12) to it.second },
                        textColor = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ranking % de Fuga Gasto Hormiga por Medio de Pago (RN-04):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MintBright
                    )
                    leakageRanking.forEach { row ->
                        Text(
                            text = "• ${row.instrument.displayName}: ${row.leakagePercentage}% (${clp(row.antExpenseAmountClp)} de ${clp(row.totalExpenseAmountClp)} CLP)",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        SankeyDiagramCanvas(categories = sankeyCategories)

        budgetViewModel?.let { bvm ->
            BudgetScreen(viewModel = bvm, embedded = true)
        }
    }
}

@Composable
private fun MiniRingColumn(
    title: String,
    items: List<Pair<String, Long>>,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.second }.coerceAtLeast(1L)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = textColor
        )
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(110.dp)) {
                var start = START_ANGLE
                items.forEachIndexed { index, pair ->
                    val sweep = FULL_CIRCLE * pair.second.toFloat() / total.toFloat()
                    drawArc(
                        color = expenseChartColors[index % expenseChartColors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 38f, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Text(
                text = clp(total),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEachIndexed { index, (label, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(expenseChartColors[index % expenseChartColors.size])
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "$label: ${clp(amount)}",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
