package cl.finantzas.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.aiep.finantzas.domain.rules.BudgetStatusEvaluator
import cl.aiep.finantzas.domain.rules.CategoryBudgetEvaluation
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.ClpImpactCalculator
import cl.aiep.finantzas.domain.rules.FinancialAnalyticsCalculator
import cl.finantzas.app.data.local.entity.BudgetLimitEntityClp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.ExpenseReaderRepository
import cl.finantzas.app.data.repository.budgetDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado inmutable de KPIs, ahorro anual proyectado y alertas activas en CLP ([Long]).
 */
data class ExpenseDashboardUiState(
    val totalMonthlyClp: Long = 0L,
    val projectedAnnualClp: Long = 0L,
    val antMonthlyClp: Long = 0L,
    val annualSavingsClp: Long = 0L,
    val formattedAnnualSavingsClp: String = ClpCurrencyFormatter.formatClp(0L),
    val budgetEvaluations: List<CategoryBudgetEvaluation> = emptyList(),
    val budgetAlertsCount: Int = 0,
    val antAlertsCount: Int = 0,
    val activeAlertsCount: Int = 0
)

class ExpenseViewModel(
    repository: ExpenseReaderRepository,
    userId: String,
    budgetLimitsFlow: Flow<List<BudgetLimitEntityClp>>,
    private val budgetEvaluator: BudgetStatusEvaluator = BudgetStatusEvaluator()
) : ViewModel() {

    constructor(
        repository: ExpenseReaderRepository,
        userId: String
    ) : this(
        repository = repository,
        userId = userId,
        budgetLimitsFlow = flowOf(emptyList()),
        budgetEvaluator = BudgetStatusEvaluator()
    )

    val expenses: StateFlow<List<ExpenseEntityClp>> =
        repository.getExpensesByUser(userId)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val totalMonthlyClp: StateFlow<Long> =
        repository.getTotalMonthlyImpactClp(userId)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ZERO_CLP
            )

    val antExpenses: StateFlow<List<ExpenseEntityClp>> =
        expenses
            .map { rows -> rows.filter { it.isAntExpense } }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val antMonthlyClp: StateFlow<Long> =
        antExpenses
            .map { rows ->
                ClpImpactCalculator.sumClp(rows.map { it.monthlyImpactClp.coerceAtLeast(ZERO_CLP) })
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ZERO_CLP
            )

    val annualSavingsClp: StateFlow<Long> =
        antExpenses
            .map { rows ->
                FinancialAnalyticsCalculator.simulateAnnualSavingsClp(
                    rows.map { it.monthlyImpactClp }
                )
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ZERO_CLP
            )

    val projectedAnnualClp: StateFlow<Long> =
        totalMonthlyClp
            .map { monthly -> ClpImpactCalculator.annualImpactClp(monthly.coerceAtLeast(ZERO_CLP)) }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ZERO_CLP
            )

    val categoryBudgetEvaluations: StateFlow<List<CategoryBudgetEvaluation>> =
        combine(
            budgetLimitsFlow.distinctUntilChanged(),
            expenses
        ) { limits, rows ->
            evaluateBudgets(limits, rows)
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val budgetAlertsCount: StateFlow<Int> =
        categoryBudgetEvaluations
            .map { evaluations -> evaluations.count { it.isAlertActive } }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ZERO_ALERTS
            )

    val activeAlertsCount: StateFlow<Int> =
        combine(budgetAlertsCount, antExpenses) { budgetAlerts, ants ->
            budgetAlerts + ants.size
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ZERO_ALERTS
            )

    val uiState: StateFlow<ExpenseDashboardUiState> =
        combine(
            totalMonthlyClp,
            projectedAnnualClp,
            antMonthlyClp,
            annualSavingsClp,
            combine(categoryBudgetEvaluations, antExpenses) { evaluations, ants ->
                evaluations to ants.size
            }
        ) { monthly, annual, antMonthly, savings, (evaluations, antCount) ->
            val budgetAlerts = evaluations.count { it.isAlertActive }
            ExpenseDashboardUiState(
                totalMonthlyClp = monthly,
                projectedAnnualClp = annual,
                antMonthlyClp = antMonthly,
                annualSavingsClp = savings,
                formattedAnnualSavingsClp = ClpCurrencyFormatter.formatClp(savings),
                budgetEvaluations = evaluations,
                budgetAlertsCount = budgetAlerts,
                antAlertsCount = antCount,
                activeAlertsCount = budgetAlerts + antCount
            )
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ExpenseDashboardUiState()
            )

    private fun evaluateBudgets(
        limits: List<BudgetLimitEntityClp>,
        rows: List<ExpenseEntityClp>
    ): List<CategoryBudgetEvaluation> {
        if (limits.isEmpty()) {
            return emptyList()
        }
        val orderedCategories = (budgetDefaults.keys + limits.map { it.category }).distinct()
        return orderedCategories.mapNotNull { category ->
            limits.firstOrNull { it.category == category }?.let { limit ->
                val matchingImpacts = rows
                    .filter { category == GLOBAL_CATEGORY || it.category == category }
                    .map { it.monthlyImpactClp.coerceAtLeast(ZERO_CLP) }
                budgetEvaluator.evaluateCategory(
                    category = category,
                    spentAmountClp = ClpImpactCalculator.sumClp(matchingImpacts),
                    budgetLimitClp = limit.monthlyLimitClp
                )
            }
        }
    }

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS: Long = 5_000L
        const val ZERO_CLP: Long = 0L
        const val ZERO_ALERTS: Int = 0
        const val GLOBAL_CATEGORY: String = "GLOBAL"
    }
}
