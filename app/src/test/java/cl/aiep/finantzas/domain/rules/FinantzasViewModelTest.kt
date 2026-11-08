package cl.aiep.finantzas.domain.rules

import cl.finantzas.app.testutil.FakeExpenseRepository
import cl.finantzas.app.testutil.MainDispatcherRule
import cl.finantzas.app.ui.viewmodel.FinantzasUiStateClp
import cl.finantzas.app.ui.viewmodel.FinantzasViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinantzasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeExpenseRepository
    private lateinit var viewModel: FinantzasViewModel

    private val coffee = expense("Café de grano", unitAmountClp = 3_500L, weeklyFrequency = 5)
    private val streaming = expense(
        concept = "Streaming",
        unitAmountClp = 26_900L,
        weeklyFrequency = FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE,
        type = ExpenseType.SUBSCRIPTION,
        category = ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS
    )
    private val lunch = expense(
        concept = "Colación",
        unitAmountClp = 3_500L,
        weeklyFrequency = 3,
        category = ExpenseCategory.FOOD_AND_GROCERIES
    )

    @Before
    fun setUp() {
        repository = FakeExpenseRepository()
        // Inyección manual por constructor: sin Hilt, sin Android.
        viewModel = FinantzasViewModel(
            ruleEngine = AntExpenseRuleEngine(),
            expenseReader = repository,
            expenseWriter = repository
        )
    }

    @Test
    fun `estado inicial es vacio y todos los montos en CLP son cero`() = runTest(mainDispatcherRule.testDispatcher) {
        advanceUntilIdle()

        assertEquals(FinantzasUiStateClp(), viewModel.uiState.value)
    }

    @Test
    fun `con StandardTestDispatcher el gasto se refleja solo despues de advanceUntilIdle`() =
        runTest(mainDispatcherRule.testDispatcher) {
            advanceUntilIdle()

            viewModel.onAddExpense(coffee)
            assertTrue(viewModel.uiState.value.expenses.isEmpty()) // la corrutina está en cola

            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.expenses.size)
        }

    @Test
    fun `calcula totales mensuales y anuales exactos en Long`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onAddExpense(coffee)    // hormiga RN-02: 70.000/mes
        viewModel.onAddExpense(streaming) // hormiga RN-03: 26.900/mes
        viewModel.onAddExpense(lunch)     // esencial:      42.000/mes
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.expenses.size)
        assertEquals(138_900L, state.totalMonthlyImpactClp)   // 70.000 + 26.900 + 42.000
        assertEquals(1_666_800L, state.totalAnnualImpactClp)  // 138.900 * 12
        assertEquals(96_900L, state.antMonthlyImpactClp)      // 70.000 + 26.900
        assertEquals(1_162_800L, state.antAnnualImpactClp)    // 96.900 * 12
        assertEquals(840_000L, state.expenses.first().annualImpactClp) // café: 70.000 * 12
        assertNull(state.errorMessage)
    }

    @Test
    fun `error de validacion se expone en errorMessage y onErrorShown lo limpia`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.validationError = "El monto debe estar entre 1 y 100000000 CLP"

            viewModel.onAddExpense(coffee)
            advanceUntilIdle()

            assertEquals("El monto debe estar entre 1 y 100000000 CLP", viewModel.uiState.value.errorMessage)
            assertTrue(viewModel.uiState.value.expenses.isEmpty())

            viewModel.onErrorShown()
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `el estado se recalcula cuando el repositorio emite datos nuevos`() =
        runTest(mainDispatcherRule.testDispatcher) {
            advanceUntilIdle()

            repository.addExpense(streaming) // escritura externa al ViewModel (otra pantalla)
            advanceUntilIdle()

            assertEquals(26_900L, viewModel.uiState.value.antMonthlyImpactClp)
            assertEquals(322_800L, viewModel.uiState.value.antAnnualImpactClp)
        }

    private fun expense(
        concept: String,
        unitAmountClp: Long,
        weeklyFrequency: Int,
        type: ExpenseType = ExpenseType.PURCHASE,
        category: ExpenseCategory = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS
    ) = ExpenseCandidate(
        concept = concept,
        unitAmountClp = unitAmountClp,
        weeklyFrequency = weeklyFrequency,
        type = type,
        category = category,
        paymentMethodCode = "DEBIT_CARD"
    )
}
