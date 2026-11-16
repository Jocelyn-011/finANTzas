
package cl.finantzas.app.ui.viewmodel

import cl.finantzas.app.data.local.dao.BudgetLimitDao
import cl.finantzas.app.data.local.entity.BudgetLimitEntityClp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.BudgetRepository
import cl.finantzas.app.data.repository.ExpenseReaderRepository
import cl.finantzas.app.testutil.MainDispatcherRule
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userId = "user-fin03"

    @Test
    fun `presupuesto se actualiza al agregar editar y eliminar gastos`() = runTest {

        val dao = FakeBudgetLimitDao()
        val expensesFlow =
            MutableStateFlow<List<ExpenseEntityClp>>(emptyList())

        val repository = BudgetRepository(dao)
        val expenseReader = FakeExpenseReader(expensesFlow)

        val viewModel = BudgetViewModel(
            repository = repository,
            expenses = expenseReader,
            userId = userId,
            scheduleSync = {}
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.cards.collect()
        }

        advanceUntilIdle()

        fun global(): BudgetCardState =
            requireNotNull(
                viewModel.cards.value.firstOrNull {
                    it.category == "GLOBAL"
                }
            )

        // 1. Verificar presupuesto inicial
        assertEquals(950_000L, global().limit)
        assertEquals(0L, global().spent)
        assertEquals(0.0, global().percentage, 0.001)

        // 2. Agregar gastos por un total de $190.000
        expensesFlow.value = listOf(
            expense("1", "FOOD_AND_GROCERIES", 150_000L),
            expense("2", "TRANSPORT_AND_MOBILITY", 40_000L)
        )

        advanceUntilIdle()

        assertEquals(190_000L, global().spent)
        assertEquals(20.0, global().percentage, 0.001)

        // Comprobar que cada categoría tiene su propio consumo
        val foodCard = viewModel.cards.value.first {
            it.category == "FOOD_AND_GROCERIES"
        }

        assertEquals(150_000L, foodCard.spent)

        val transportCard = viewModel.cards.value.first {
            it.category == "TRANSPORT_AND_MOBILITY"
        }

        assertEquals(40_000L, transportCard.spent)

        // 3. Editar gasto: nuevo total $250.000
        expensesFlow.value = listOf(
            expense("1", "FOOD_AND_GROCERIES", 210_000L),
            expense("2", "TRANSPORT_AND_MOBILITY", 40_000L)
        )

        advanceUntilIdle()

        assertEquals(250_000L, global().spent)
        assertEquals(
            250_000.0 / 950_000.0 * 100,
            global().percentage,
            0.001
        )

        // 4. Simular eliminación y actualización de gastos
        expensesFlow.value = listOf(
            expense("1", "FOOD_AND_GROCERIES", 100_000L)
        )

        advanceUntilIdle()

        assertEquals(100_000L, global().spent)
        assertEquals(0L, viewModel.cards.value.first {
            it.category == "TRANSPORT_AND_MOBILITY"
        }.spent)

        // 5. Cambiar el presupuesto global a $500.000
        viewModel.save("GLOBAL", "500000")

        advanceUntilIdle()

        assertEquals(500_000L, global().limit)
        assertEquals(100_000L, global().spent)
        assertEquals(20.0, global().percentage, 0.001)
    }

    private fun expense(
        id: String,
        category: String,
        amount: Long
    ): ExpenseEntityClp = ExpenseEntityClp(
        id = id,
        userId = userId,
        concept = "Gasto de prueba $id",
        unitAmountClp = amount,
        monthlyImpactClp = amount,
        weeklyFrequency = 0,
        expenseType = "PURCHASE",
        category = category,
        paymentMethod = "DEBIT_CARD",
        isAntExpense = false,
        triggeredRuleCode = "ESSENTIAL",
        ruleReason = "Prueba FIN-03",
        createdAt = 1L
    )

    private class FakeBudgetLimitDao : BudgetLimitDao {

        private val budgets =
            MutableStateFlow<List<BudgetLimitEntityClp>>(emptyList())

        override fun observe(
            userId: String
        ): Flow<List<BudgetLimitEntityClp>> =
            budgets.map { rows ->
                rows.filter { it.userId == userId }
            }

        override suspend fun snapshot(
            userId: String
        ): List<BudgetLimitEntityClp> =
            budgets.value.filter { it.userId == userId }

        override suspend fun insertMissing(
            rows: List<BudgetLimitEntityClp>
        ) {
            val current = budgets.value.toMutableList()

            rows.forEach { row ->
                if (current.none {
                    it.userId == row.userId &&
                        it.category == row.category
                }) {
                    current.add(row)
                }
            }

            budgets.value = current
        }

        override suspend fun edit(
            userId: String,
            category: String,
            amount: Long
        ) {
            budgets.value = budgets.value.map { row ->
                if (row.userId == userId &&
                    row.category == category
                ) {
                    row.copy(
                        monthlyLimitClp = amount,
                        revision = row.revision + 1,
                        dirty = true
                    )
                } else {
                    row
                }
            }
        }

        override suspend fun acceptRemote(
            userId: String,
            category: String,
            amount: Long
        ) {
            budgets.value = budgets.value.map { row ->
                if (row.userId == userId &&
                    row.category == category &&
                    !row.dirty
                ) {
                    row.copy(monthlyLimitClp = amount)
                } else {
                    row
                }
            }
        }

        override suspend fun acknowledge(
            userId: String,
            category: String,
            revision: Long
        ) {
            budgets.value = budgets.value.map { row ->
                if (row.userId == userId &&
                    row.category == category &&
                    row.revision == revision
                ) {
                    row.copy(dirty = false)
                } else {
                    row
                }
            }
        }
    }

    private class FakeExpenseReader(
        private val expenses:
            MutableStateFlow<List<ExpenseEntityClp>>
    ) : ExpenseReaderRepository {

        override fun getExpensesByUser(
            userId: String
        ): Flow<List<ExpenseEntityClp>> =
            expenses.map { rows ->
                rows.filter { it.userId == userId }
            }

        override suspend fun getExpenseById(
            expenseId: String,
            userId: String
        ): ExpenseEntityClp? =
            expenses.value.firstOrNull {
                it.id == expenseId &&
                    it.userId == userId
            }

        override fun getTotalMonthlyImpactClp(
            userId: String
        ): Flow<Long> =
            getExpensesByUser(userId).map { rows ->
                rows.sumOf { it.monthlyImpactClp }
            }
    }
}
