package cl.finantzas.app.data.repository

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EncryptedExpensePerformanceTest {

    @Test
    fun encryptedExpenseWritesUnder50ms() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val database = SecureDatabaseProvider.getDatabase(context)
        val dao = database.expenseDao()
        val repository: ExpenseWriterRepository = RoomExpenseRepository(dao)

        val userId = "performance-test-user"
        val timesMs = mutableListOf<Double>()

        // Abrir y preparar la base antes de medir
        dao.getExpenseById("warmup", userId)

        repeat(20) { index ->
            val expense = ExpenseEntityClp(
                id = UUID.randomUUID().toString(),
                userId = userId,
                concept = "Prueba CLP $index",
                unitAmountClp = 1500L,
                monthlyImpactClp = 6000L,
                weeklyFrequency = 1,
                expenseType = "VARIABLE",
                category = "TEST",
                paymentMethod = "CASH",
                isAntExpense = false,
                triggeredRuleCode = "",
                ruleReason = "",
                createdAt = System.currentTimeMillis()
            )

            val start = SystemClock.elapsedRealtimeNanos()
            repository.insert(expense)
            val elapsedMs =
                (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0

            timesMs.add(elapsedMs)

            assertNotNull(dao.getExpenseById(expense.id, userId))
        }

        val maximumMs = timesMs.maxOrNull() ?: 0.0
        val averageMs = timesMs.average()

        println("FINANTZAS_WRITE_AVG_MS=$averageMs")
        println("FINANTZAS_WRITE_MAX_MS=$maximumMs")

        assertTrue(
            "Una escritura supero los 50 ms: $maximumMs ms",
            maximumMs < 50.0
        )
    }
}