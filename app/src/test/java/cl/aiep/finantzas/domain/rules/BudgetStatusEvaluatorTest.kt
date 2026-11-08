package cl.aiep.finantzas.domain.rules

import cl.finantzas.app.data.local.entity.BudgetLimitEntityClp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.ExpenseReaderRepository
import cl.finantzas.app.testutil.MainDispatcherRule
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetStatusEvaluatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val evaluator = BudgetStatusEvaluator()

    // ------------------------------------------------------------------------
    // 1. Análisis de Valores Límite: Umbral Inferior de Alerta (75%)
    // ------------------------------------------------------------------------
    @Test
    fun `cuando ejecucion es 74 porciento retorna estado CONTROLADO Verde sin alerta activa`() {
        val evaluation = evaluator.evaluateCategory(
            category = "FOOD_AND_GROCERIES",
            spentAmountClp = 74_000L,
            budgetLimitClp = 100_000L
        )

        assertEquals(74, evaluation.consumedPercentage)
        assertEquals(BudgetHealthStatus.CONTROLADO, evaluation.status)
        assertEquals("Controlado", evaluation.status.displayLabel)
        assertFalse(evaluation.isAlertActive)
    }

    @Test
    fun `cuando ejecucion alcanza 75 porciento exacto retorna ALERTA_PREVENTIVA Ambar`() {
        val evaluation = evaluator.evaluateCategory(
            category = "FOOD_AND_GROCERIES",
            spentAmountClp = 75_000L,
            budgetLimitClp = 100_000L
        )

        assertEquals(75, evaluation.consumedPercentage)
        assertEquals(BudgetHealthStatus.ALERTA_PREVENTIVA, evaluation.status)
        assertEquals("Alerta Preventiva", evaluation.status.displayLabel)
        assertTrue(evaluation.isAlertActive)
    }

    // ------------------------------------------------------------------------
    // 2. Análisis de Valores Límite: Umbral de Exceso de Presupuesto (100%)
    // ------------------------------------------------------------------------
    @Test
    fun `cuando ejecucion es 99 porciento permanece en ALERTA_PREVENTIVA Ambar`() {
        val evaluation = evaluator.evaluateCategory(
            category = "MICRO_PURCHASES_AND_SNACKS",
            spentAmountClp = 99_000L,
            budgetLimitClp = 100_000L
        )

        assertEquals(99, evaluation.consumedPercentage)
        assertEquals(BudgetHealthStatus.ALERTA_PREVENTIVA, evaluation.status)
        assertEquals("Alerta Preventiva", evaluation.status.displayLabel)
        assertTrue(evaluation.isAlertActive)
    }

    @Test
    fun `cuando ejecucion alcanza 100 porciento exacto retorna PRESUPUESTO_EXCEDIDO Rojo`() {
        val evaluation = evaluator.evaluateCategory(
            category = "ENTERTAINMENT_SUBSCRIPTIONS",
            spentAmountClp = 100_000L,
            budgetLimitClp = 100_000L
        )

        assertEquals(100, evaluation.consumedPercentage)
        assertEquals(BudgetHealthStatus.PRESUPUESTO_EXCEDIDO, evaluation.status)
        assertEquals("Presupuesto Excedido", evaluation.status.displayLabel)
        assertTrue(evaluation.isAlertActive)
    }

    @Test
    fun `cuando ejecucion supera 100 porciento con 120000 CLP sobre 100000 CLP retorna PRESUPUESTO_EXCEDIDO Rojo`() {
        val evaluation = evaluator.evaluateCategory(
            category = "ENTERTAINMENT_SUBSCRIPTIONS",
            spentAmountClp = 120_000L,
            budgetLimitClp = 100_000L
        )

        assertEquals(120, evaluation.consumedPercentage)
        assertEquals(BudgetHealthStatus.PRESUPUESTO_EXCEDIDO, evaluation.status)
        assertEquals("Presupuesto Excedido", evaluation.status.displayLabel)
        assertTrue(evaluation.isAlertActive)
    }

    // ------------------------------------------------------------------------
    // 3. Guardia contra División por Cero y Extensibilidad SOLID OCP
    // ------------------------------------------------------------------------
    @Test
    fun `cuando presupuesto limite es cero o negativo aplica guardia sin ArithmeticException`() {
        val zeroLimitEval = evaluator.evaluateCategory(
            category = "TRANSPORT_AND_MOBILITY",
            spentAmountClp = 50_000L,
            budgetLimitClp = 0L
        )
        val negativeLimitEval = evaluator.evaluateCategory(
            category = "TRANSPORT_AND_MOBILITY",
            spentAmountClp = 50_000L,
            budgetLimitClp = -10_000L
        )

        assertEquals(0, zeroLimitEval.consumedPercentage)
        assertEquals(BudgetHealthStatus.CONTROLADO, zeroLimitEval.status)
        assertEquals(0, negativeLimitEval.consumedPercentage)
        assertEquals(BudgetHealthStatus.CONTROLADO, negativeLimitEval.status)
    }

    @Test
    fun `permite extender politica de umbrales mediante fun interface BudgetThresholdPolicy sin modificar evaluador`() {
        val strictPolicy = BudgetThresholdPolicy { percentage ->
            when {
                percentage >= 90 -> BudgetHealthStatus.PRESUPUESTO_EXCEDIDO
                percentage >= 50 -> BudgetHealthStatus.ALERTA_PREVENTIVA
                else -> BudgetHealthStatus.CONTROLADO
            }
        }
        val customEvaluator = BudgetStatusEvaluator(policy = strictPolicy)

        val result = customEvaluator.evaluateCategory("GLOBAL", spentAmountClp = 60_000L, budgetLimitClp = 100_000L)

        assertEquals(60, result.consumedPercentage)
        assertEquals(BudgetHealthStatus.ALERTA_PREVENTIVA, result.status)
    }

    // ------------------------------------------------------------------------
    // 4. Proyección de Ahorro Anual (simulateAnnualSavingsClp) y Flujo Reactivo
    // ------------------------------------------------------------------------
    @Test
    fun `simulateAnnualSavingsClp proyecta correctamente el ahorro anual en CLP Long a partir de gastos hormiga`() {
        // Caso individual: $70.000 CLP/mes * 12 meses = $840.000 CLP/año
        assertEquals(840_000L, FinancialAnalyticsCalculator.simulateAnnualSavingsClp(70_000L))

        // Caso lista de impactos mensuales hormiga: ($70.000 + $26.900) * 12 = $1.162.800 CLP/año
        val antImpacts = listOf(70_000L, 26_900L)
        val projectedAnnualClp = FinancialAnalyticsCalculator.simulateAnnualSavingsClp(antImpacts)
        assertEquals(1_162_800L, projectedAnnualClp)
        assertEquals("$1.162.800", ClpCurrencyFormatter.formatClp(projectedAnnualClp))

        // Caso borde: monto cero o negativo retorna 0L
        assertEquals(0L, FinancialAnalyticsCalculator.simulateAnnualSavingsClp(0L))
        assertEquals(0L, FinancialAnalyticsCalculator.simulateAnnualSavingsClp(-5_000L))
    }

    @Test
    fun `ExpenseViewModel conecta reactivamente simulateAnnualSavingsClp y conteo de alertas activas`() = runTest {
        val userId = "user-test"
        val expensesFlow = MutableStateFlow<List<ExpenseEntityClp>>(emptyList())
        val budgetsFlow = MutableStateFlow(
            listOf(
                BudgetLimitEntityClp(userId, "FOOD_AND_GROCERIES", 100_000L),
                BudgetLimitEntityClp(userId, "ENTERTAINMENT_SUBSCRIPTIONS", 25_000L)
            )
        )
        val fakeReader = createFakeReader(expensesFlow)
        val viewModel = ExpenseViewModel(fakeReader, userId, budgetsFlow)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.activeAlertsCount.collect() }
        advanceUntilIdle()

        assertEquals(0L, viewModel.uiState.value.annualSavingsClp)
        assertEquals(0, viewModel.uiState.value.activeAlertsCount)

        expensesFlow.value = listOf(
            buildTestExpense(id = "1", category = "FOOD_AND_GROCERIES", monthlyClp = 75_000L, isAnt = false),
            buildTestExpense(id = "2", category = "ENTERTAINMENT_SUBSCRIPTIONS", monthlyClp = 26_900L, isAnt = true)
        )
        advanceUntilIdle()

        val updated = viewModel.uiState.value
        assertEquals(322_800L, updated.annualSavingsClp)
        assertEquals("$322.800", updated.formattedAnnualSavingsClp)
        assertEquals(2, updated.budgetAlertsCount)
        assertEquals(1, updated.antAlertsCount)
        assertEquals(3, updated.activeAlertsCount)
        assertEquals(3, viewModel.activeAlertsCount.value)

        val legacyViewModel = ExpenseViewModel(repository = fakeReader, userId = userId)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { legacyViewModel.annualSavingsClp.collect() }
        advanceUntilIdle()
        assertEquals(322_800L, legacyViewModel.annualSavingsClp.value)
    }

    @Test
    fun `metodos estaticos y simulateAnnualSavingsFromVerdicts mantienen consistencia en CLP`() {
        assertEquals(
            BudgetHealthStatus.ALERTA_PREVENTIVA,
            BudgetStatusEvaluator.evaluateStatus(spentAmountClp = 80_000L, budgetLimitClp = 100_000L)
        )
        assertEquals(
            100,
            BudgetStatusEvaluator.evaluateCategory("GLOBAL", 100_000L, 100_000L).consumedPercentage
        )

        val verdicts = listOf(
            AntExpenseVerdict(isAntExpense = true, triggeredRuleCode = "RN-02", monthlyImpactClp = 70_000L),
            AntExpenseVerdict(isAntExpense = false, triggeredRuleCode = "ESSENTIAL", monthlyImpactClp = 250_000L)
        )
        assertEquals(840_000L, FinancialAnalyticsCalculator.simulateAnnualSavingsFromVerdicts(verdicts))
    }

    private fun createFakeReader(
        expensesFlow: MutableStateFlow<List<ExpenseEntityClp>>
    ): ExpenseReaderRepository = object : ExpenseReaderRepository {
        override fun getExpensesByUser(userId: String): Flow<List<ExpenseEntityClp>> = expensesFlow
        override suspend fun getExpenseById(expenseId: String, userId: String): ExpenseEntityClp? = null
        override fun getTotalMonthlyImpactClp(userId: String): Flow<Long> =
            expensesFlow.map { rows -> rows.sumOf { it.monthlyImpactClp } }
    }

    private fun buildTestExpense(
        id: String,
        category: String,
        monthlyClp: Long,
        isAnt: Boolean
    ): ExpenseEntityClp = ExpenseEntityClp(
        id = id,
        userId = "user-test",
        concept = "Gasto $id",
        unitAmountClp = monthlyClp,
        monthlyImpactClp = monthlyClp,
        weeklyFrequency = 0,
        expenseType = if (isAnt) "SUBSCRIPTION" else "PURCHASE",
        category = category,
        paymentMethod = "DEBIT_CARD",
        isAntExpense = isAnt,
        triggeredRuleCode = if (isAnt) "RN-03" else "ESSENTIAL",
        ruleReason = "Prueba",
        createdAt = 1L
    )
}
