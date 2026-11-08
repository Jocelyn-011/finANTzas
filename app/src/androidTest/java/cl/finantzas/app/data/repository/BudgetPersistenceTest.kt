package cl.finantzas.app.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.finantzas.app.data.local.database.FinantzasDatabase
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BudgetPersistenceTest {
    @Test fun encryptedBudgetsPersistAndPendingEditsSurviveRemoteMerge() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "budget-test-${UUID.randomUUID()}.db"
        val passphrase = UUID.randomUUID().toString().toByteArray()
        System.loadLibrary("sqlcipher")
        fun open() = Room.databaseBuilder(context, FinantzasDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
            .addMigrations(cl.finantzas.app.data.local.database.SecureDatabaseProvider.BUDGET_MIGRATION).build()
        val legacy = SupportOpenHelperFactory(passphrase.copyOf()).create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE expenses (id TEXT NOT NULL PRIMARY KEY, user_id TEXT NOT NULL, concept TEXT NOT NULL, unit_amount_clp INTEGER NOT NULL, monthly_impact_clp INTEGER NOT NULL, weekly_frequency INTEGER NOT NULL, expense_type TEXT NOT NULL, category TEXT NOT NULL, payment_method TEXT NOT NULL, is_ant_expense INTEGER NOT NULL, triggered_rule_code TEXT NOT NULL, rule_reason TEXT NOT NULL, created_at INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO expenses VALUES ('legacy', 'a', 'Legacy expense', 1000, 1000, 0, 'PURCHASE', 'FOOD_AND_GROCERIES', 'CASH', 0, 'ESSENTIAL', '', 0)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        legacy.writableDatabase
        legacy.close()
        assertFalse(context.getDatabasePath(name).inputStream().use {
            String(it.readBytes().take(16).toByteArray(), Charsets.US_ASCII)
        }.startsWith("SQLite format 3"))
        var db = open()
        try {
            assertNotNull(db.expenseDao().getExpenseById("legacy", "a"))
            var repo = BudgetRepository(db.budgetLimitDao())
            repo.initialize("a"); repo.initialize("a"); repo.initialize("b")
            assertEquals(6, db.budgetLimitDao().snapshot("a").size)
            repo.save("a", "GLOBAL", 5_000_000_000L)
            val revision = db.budgetLimitDao().snapshot("a").first { it.category == "GLOBAL" }.revision
            repo.save("a", "GLOBAL", 6_000_000_000L)
            db.budgetLimitDao().acknowledge("a", "GLOBAL", revision)
            db.budgetLimitDao().acceptRemote("a", "GLOBAL", 10L)
            db.close(); db = open(); repo = BudgetRepository(db.budgetLimitDao())
            val saved = repo.observe("a").first().first { it.category == "GLOBAL" }
            assertEquals(6_000_000_000L, saved.monthlyLimitClp)
            assertTrue(saved.dirty)
            assertEquals(950_000L, db.budgetLimitDao().snapshot("b").first { it.category == "GLOBAL" }.monthlyLimitClp)
            try { repo.save("a", "GLOBAL", 0); fail("zero accepted") } catch (_: IllegalArgumentException) { }
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun expenseInsertUpdatesBudgetStateWithoutRestart() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val db = Room.inMemoryDatabaseBuilder(context, FinantzasDatabase::class.java).build()
            val store = androidx.lifecycle.ViewModelStore()
            try {
                val repo = BudgetRepository(db.budgetLimitDao())
                repo.initialize("a")
                val vm = cl.finantzas.app.ui.viewmodel.BudgetViewModel(repo, RoomExpenseRepository(db.expenseDao()), "a") { }
                store.put("budget", vm)
                val state = vm.cards
                withTimeout(5000) { state.first { cards -> cards.any { it.category == "FOOD_AND_GROCERIES" && it.percentage == 0.0 } } }
                db.expenseDao().insert(ExpenseEntityClp("expense", "a", "Alimentos", 140_000, 140_000,
                    0, "PURCHASE", "FOOD_AND_GROCERIES", "CASH", false, "ESSENTIAL", "", 0))
                withTimeout(5000) { state.first { cards -> cards.any { it.category == "FOOD_AND_GROCERIES" && it.percentage == 50.0 } } }
                repo.save("a", "FOOD_AND_GROCERIES", 140_000)
                withTimeout(5000) { state.first { cards -> cards.any { it.category == "FOOD_AND_GROCERIES" && it.percentage == 100.0 } } }
            } finally { store.clear(); db.close() }
        }
    }
}
