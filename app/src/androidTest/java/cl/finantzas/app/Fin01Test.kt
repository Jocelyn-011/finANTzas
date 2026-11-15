package cl.finantzas.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cl.finantzas.app.data.local.database.FinantzasDatabase
import cl.finantzas.app.data.demo.ExpenseDemoDataClp
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.components.ExpenseRegistrationForm
import cl.finantzas.app.ui.components.LOCAL_EXPENSE_USER
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class Fin01Test {
    @get:Rule val compose = createComposeRule()

    @Test fun formValidationAndSuccessfulReset() {
        var saves = 0
        compose.setContent { MaterialTheme { ExpenseRegistrationForm { _, _ -> saves++; null } } }
        val button = compose.onNodeWithText("+ Registrar Gasto")
        button.assertIsNotEnabled()
        compose.onNodeWithText("Concepto").performTextInput(" a ")
        button.assertIsNotEnabled()
        compose.onNodeWithText("Concepto").performTextReplacement("Cafe")
        compose.onNodeWithText("Monto ($)").performTextReplacement("0")
        button.assertIsNotEnabled()
        compose.onNodeWithText("Monto ($)").performTextReplacement("1500")
        button.assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(1, saves)
        compose.onNodeWithText("Monto ($)").assertTextContains("3500")
        button.assertIsNotEnabled()
    }

    @Test fun failedSavePreservesInputAndSavingDisablesButton() {
        val gate = kotlinx.coroutines.CompletableDeferred<String?>()
        compose.setContent { MaterialTheme { ExpenseRegistrationForm { _, _ -> gate.await() } } }
        compose.onNodeWithText("Concepto").performTextInput("Cafe")
        compose.onNodeWithText("Monto ($)").performTextReplacement("1500")
        compose.onNodeWithText("+ Registrar Gasto").performClick()
        compose.onNodeWithText("Guardando\u2026").assertIsNotEnabled()
        gate.complete("Error de prueba")
        compose.waitForIdle()
        compose.onNodeWithText("Concepto").assertTextContains("Cafe")
        compose.onNodeWithText("Monto ($)").assertTextContains("1500")
        compose.onNodeWithText("+ Registrar Gasto").assertIsEnabled()
    }

    @Test fun roomCrudIsolationAndSafeRestore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, FinantzasDatabase::class.java).build()
        try {
            val dao = db.expenseDao()
            val repo = RoomExpenseRepository(dao)
            val real = ExpenseDemoDataClp.create(LOCAL_EXPENSE_USER).first().copy(id = "real")
            repo.insert(real)
            assertEquals(listOf(real), repo.getExpensesByUser(real.userId).first())
            assertTrue(repo.getExpensesByUser("other").first().isEmpty())
            val updated = real.copy(concept = "Actualizado")
            repo.update(updated)
            assertEquals(updated, repo.getExpensesByUser(real.userId).first().single())
            for (action in listOf<suspend () -> Unit>(
                { repo.insert(real.copy(userId = "other")) },
                { repo.update(real.copy(userId = "other")) },
                { repo.delete(real.copy(userId = "other")) },
                { repo.restoreDemoExpenses("other") }
            )) {
                try { action(); fail("Operaci?n entre usuarios aceptada") }
                catch (_: IllegalArgumentException) { }
            }
            repo.restoreDemoExpenses(real.userId)
            repo.restoreDemoExpenses(real.userId)
            assertEquals(5, repo.getExpensesByUser(real.userId).first().size)
            assertEquals(updated, repo.getExpenseById(real.id, real.userId))
            repo.delete(updated)
            assertNull(repo.getExpenseById(real.id, real.userId))
        } finally { db.close() }
    }
}
