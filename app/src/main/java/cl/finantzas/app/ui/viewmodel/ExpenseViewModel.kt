package cl.finantzas.app.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import cl.aiep.finantzas.domain.rules.ClpImpactCalculator
import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.FinancialAnalyticsCalculator
import cl.aiep.finantzas.domain.rules.PaymentInstrument
import cl.aiep.finantzas.domain.rules.PaymentMethodLeakageResult
import cl.finantzas.app.data.local.entity.ExpenseDemoDataClp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.ui.components.DonutSliceUiModel
import cl.finantzas.app.ui.theme.AlertaHormiga
import cl.finantzas.app.ui.theme.Emerald
import cl.finantzas.app.ui.theme.Petrol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado inmutable de la pantalla de gráficos y analíticas en CLP.
 */
data class ExpenseChartsUiState(
    val categorySlices: List<DonutSliceUiModel> = emptyList(),
    val paymentSlices: List<DonutSliceUiModel> = emptyList(),
    val paymentLeakageRanking: List<PaymentMethodLeakageResult> = emptyList(),
    val totalMonthlyClp: Long = 0L,
    val totalAntExpenseClp: Long = 0L
)

class ExpenseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(buildChartsUiState(ExpenseDemoDataClp.initialExpenses))
    val uiState: StateFlow<ExpenseChartsUiState> = _uiState.asStateFlow()

    private fun buildChartsUiState(expenses: List<ExpenseEntityClp>): ExpenseChartsUiState {
        val totalMonthlyClp = expenses.fold(0L) { acc, item ->
            acc + ClpImpactCalculator.monthlyImpactClp(item.unitAmountClp, item.weeklyFrequency)
        }

        val antMonthlyClp = expenses.filter { it.isAntExpense }.fold(0L) { acc, item ->
            acc + ClpImpactCalculator.monthlyImpactClp(item.unitAmountClp, item.weeklyFrequency)
        }

        // 1. Slices por Categoría
        val categoryTotals = expenses.groupBy { it.category }.mapValues { (_, items) ->
            items.fold(0L) { acc, e ->
                acc + ClpImpactCalculator.monthlyImpactClp(e.unitAmountClp, e.weeklyFrequency)
            }
        }

        val categoryColors = mapOf(
            ExpenseCategory.MICRO_PURCHASES_AND_SNACKS to Color(0xFFF59E0B),
            ExpenseCategory.FOOD_AND_GROCERIES to Emerald,
            ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS to AlertaHormiga
        )

        val categorySlices = categoryTotals.map { (cat, amount) ->
            val sweep = if (totalMonthlyClp > 0L) (amount.toFloat() / totalMonthlyClp) * 100f else 0f
            val label = when (cat) {
                ExpenseCategory.MICRO_PURCHASES_AND_SNACKS -> "Micro-compras y Snacks"
                ExpenseCategory.FOOD_AND_GROCERIES -> "Alimentación y Supermercado"
                ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS -> "Suscripciones Entretenimiento"
            }
            DonutSliceUiModel(label, amount, sweep, categoryColors[cat] ?: Petrol)
        }

        // 2. Slices por Medio de Pago
        val paymentTotals = expenses.groupBy { it.paymentInstrument }.mapValues { (_, items) ->
            items.fold(0L) { acc, e ->
                acc + ClpImpactCalculator.monthlyImpactClp(e.unitAmountClp, e.weeklyFrequency)
            }
        }

        val paymentColors = mapOf(
            PaymentInstrument.CreditCard to Petrol,
            PaymentInstrument.DebitCard to Emerald,
            PaymentInstrument.NfcMobileWallet to Color(0xFF6366F1),
            PaymentInstrument.Cash to Color(0xFFF59E0B),
            PaymentInstrument.BankTransfer to Color(0xFF0D9488)
        )

        val paymentSlices = paymentTotals.map { (instrument, amount) ->
            val sweep = if (totalMonthlyClp > 0L) (amount.toFloat() / totalMonthlyClp) * 100f else 0f
            DonutSliceUiModel(instrument.displayName, amount, sweep, paymentColors[instrument] ?: Petrol)
        }

        // 3. Ranking de Porcentaje de Fuga (RN-04 - LSP)
        val ranking = paymentTotals.map { (instrument, totalAmount) ->
            val antAmount = expenses
                .filter { it.paymentInstrument == instrument && it.isAntExpense }
                .fold(0L) { acc, e ->
                    acc + ClpImpactCalculator.monthlyImpactClp(e.unitAmountClp, e.weeklyFrequency)
                }

            FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
                instrument = instrument,
                totalExpenseAmountClp = totalAmount,
                antExpenseAmountClp = antAmount
            )
        }.sortedByDescending { it.leakagePercentage }

        return ExpenseChartsUiState(
            categorySlices = categorySlices,
            paymentSlices = paymentSlices,
            paymentLeakageRanking = ranking,
            totalMonthlyClp = totalMonthlyClp,
            totalAntExpenseClp = antMonthlyClp
        )
    }
}