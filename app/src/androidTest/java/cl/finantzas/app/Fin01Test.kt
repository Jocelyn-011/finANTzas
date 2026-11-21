package cl.finantzas.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cl.finantzas.app.data.local.database.FinantzasDatabase
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
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
            val real = ExpenseDemoDataClp.create(LOCAL_EXPENSE_USER).first().copy(id = "demo-cafe", isDemo = false)
            repo.insert(real)
            assertEquals(listOf(real), repo.getExpensesByUser(real.userId).first())
            assertTrue(repo.getExpensesByUser("other").first().isEmpty())
            val updated = real.copy(concept = "Actualizado")
            repo.update(updated)
            assertEquals(updated, repo.getExpensesByUser(real.userId).first().single())
            for (action in listOf<suspend () -> Unit>(
                { repo.insert(real.copy(userId = "other")) },
                { repo.update(real.copy(userId = "other")) },
                { repo.delete(real.copy(userId = "other")) }
            )) {
                try { action(); fail("Operaci?n entre usuarios aceptada") }
                catch (_: IllegalArgumentException) { }
            }
            repo.restoreDemoExpenses(real.userId)
            repo.restoreDemoExpenses(real.userId)
            repo.restoreDemoExpenses("other")
            repo.restoreDemoExpenses("other")
            assertEquals(5, repo.getExpensesByUser(real.userId).first().size)
            assertEquals(4, repo.getExpensesByUser("other").first().size)
            assertTrue(repo.getExpensesByUser("other").first().all { it.isDemo })
            assertEquals(listOf(updated), dao.getExpensesForSync(real.userId))
            assertTrue(dao.getExpensesForSync("other").isEmpty())
            assertEquals(updated, repo.getExpenseById(real.id, real.userId))
            repo.delete(updated)
            assertNull(repo.getExpenseById(real.id, real.userId))
        } finally { db.close() }
    }

    @Test fun restoredExamplesSurviveDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fin01-test-${java.util.UUID.randomUUID()}.db"
        try {
            val first = Room.databaseBuilder(context, FinantzasDatabase::class.java, name).build()
            RoomExpenseRepository(first.expenseDao()).restoreDemoExpenses("authenticated")
            first.close()
            val reopened = Room.databaseBuilder(context, FinantzasDatabase::class.java, name).build()
            try {
                val rows = reopened.expenseDao().getExpensesByUser("authenticated").first()
                assertEquals(4, rows.size)
                assertTrue(rows.all { it.isDemo })
                assertTrue(reopened.expenseDao().getExpensesForSync("authenticated").isEmpty())
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun migrationPreservesLegacyExpenseWithoutClassifyingIt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "fin01-migration-${java.util.UUID.randomUUID()}.db"
        try {
            val path = context.getDatabasePath(name)
            path.parentFile?.mkdirs()
            val old = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null)
            old.execSQL("CREATE TABLE expenses (id TEXT NOT NULL PRIMARY KEY, user_id TEXT NOT NULL, concept TEXT NOT NULL, unit_amount_clp INTEGER NOT NULL, monthly_impact_clp INTEGER NOT NULL, weekly_frequency INTEGER NOT NULL, expense_type TEXT NOT NULL, category TEXT NOT NULL, payment_method TEXT NOT NULL, is_ant_expense INTEGER NOT NULL, triggered_rule_code TEXT NOT NULL, rule_reason TEXT NOT NULL, created_at INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE category_budgets (user_id TEXT NOT NULL, category TEXT NOT NULL, monthly_limit_clp INTEGER NOT NULL, revision INTEGER NOT NULL, dirty INTEGER NOT NULL, PRIMARY KEY(user_id, category))")
            old.execSQL("INSERT INTO expenses VALUES ('demo-cafe', 'authenticated', 'Legacy', 3500, 3500, 0, 'FIXED', 'Food', 'CASH', 0, '', '', 1)")
            old.version = 2
            old.close()
            val db = Room.databaseBuilder(context, FinantzasDatabase::class.java, name)
                .addMigrations(SecureDatabaseProvider.EXPENSE_DEMO_MIGRATION).build()
            try {
                val row = db.expenseDao().getExpenseById("demo-cafe", "authenticated")!!
                assertFalse(row.isDemo)
                assertEquals(3500L, row.unitAmountClp)
                RoomExpenseRepository(db.expenseDao()).restoreDemoExpenses("authenticated")
                assertEquals(row, db.expenseDao().getExpenseById(row.id, row.userId))
                assertEquals(listOf(row), db.expenseDao().getExpensesForSync(row.userId))
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
