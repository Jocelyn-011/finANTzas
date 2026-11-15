package cl.finantzas.app

import cl.finantzas.app.data.demo.ExpenseDemoDataClp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.repository.ExpenseReaderRepository
import cl.finantzas.app.data.repository.ExpenseWriterRepository
import cl.finantzas.app.testutil.MainDispatcherRule
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Fin01ViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class Repository : ExpenseReaderRepository, ExpenseWriterRepository {
        val rows = MutableStateFlow<List<ExpenseEntityClp>>(emptyList())
        var failWrite = false
        override fun getExpensesByUser(userId: String) = rows.map { it.filter { row -> row.userId == userId } }
        override fun getTotalMonthlyImpactClp(userId: String) = getExpensesByUser(userId).map { it.sumOf { row -> row.monthlyImpactClp } }
        override suspend fun getExpenseById(expenseId: String, userId: String) = rows.value.find { it.id == expenseId && it.userId == userId }
        override suspend fun insert(expense: ExpenseEntityClp) {
            check(!failWrite)
            rows.value = rows.value + expense
        }
        override suspend fun update(expense: ExpenseEntityClp) { rows.value = rows.value.map { if (it.id == expense.id) expense else it } }
        override suspend fun delete(expense: ExpenseEntityClp) { rows.value = rows.value.filterNot { it.id == expense.id } }
    }

    @Test fun crudUpdatesObservedHistoryAndRejectsOtherUsers() = runTest {
        val repo = Repository()
        val vm = ExpenseViewModel(repo, "a")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.expenses.collect() }
        val row = ExpenseDemoDataClp.create("a").first().copy(id = "real")
        vm.insert(row)
        runCurrent()
        assertEquals(listOf(row), vm.expenses.value)
        val updated = row.copy(concept = "Actualizado")
        vm.update(updated)
        runCurrent()
        assertEquals(listOf(updated), vm.expenses.value)
        for (action in listOf<suspend () -> Unit>(
            { vm.insert(row.copy(userId = "b")) },
            { vm.update(row.copy(userId = "b")) },
            { vm.delete(row.copy(userId = "b")) },
            { vm.restoreDemoExpenses() }
        )) {
            try { action(); fail("Usuario incorrecto aceptado") }
            catch (_: IllegalArgumentException) { }
        }
        vm.delete(updated)
        runCurrent()
        assertTrue(vm.expenses.value.isEmpty())
    }

    @Test fun failedInsertDoesNotChangeHistoryAndCanBeRetried() = runTest {
        val repo = Repository()
        val vm = ExpenseViewModel(repo, "a")
        val row = ExpenseDemoDataClp.create("a").first()
        repo.failWrite = true
        try { vm.insert(row); fail("Error no propagado") } catch (_: IllegalStateException) { }
        assertTrue(repo.rows.value.isEmpty())
        repo.failWrite = false
        vm.insert(row)
        assertEquals(listOf(row), repo.rows.value)
    }
}
