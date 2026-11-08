package cl.finantzas.app.data.repository

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

        val initializationStart = SystemClock.elapsedRealtimeNanos()
        val database = SecureDatabaseProvider.getDatabase(context)
        val dao = database.expenseDao()
        val repository: ExpenseWriterRepository = RoomExpenseRepository(dao)

        val userId = "performance-test-${UUID.randomUUID()}"
        val timesMs = mutableListOf<Double>()

        // This awaited query opens Room/SQLCipher before any insert is timed.
        dao.getExpenseById("warmup", userId)
        val initializationMs = (SystemClock.elapsedRealtimeNanos() - initializationStart) / 1_000_000.0
        println("FINANTZAS_INITIALIZATION_MS=$initializationMs")

        val header = context.getDatabasePath("finantzas_secure.db").inputStream().use {
            val bytes = ByteArray(16)
            assertTrue("Database header must exist", it.read(bytes) == bytes.size)
            bytes.toString(Charsets.US_ASCII)
        }
        assertFalse("Database must be encrypted", header.startsWith("SQLite format 3"))

        fun expense(index: Int) = ExpenseEntityClp(
            id = UUID.randomUUID().toString(), userId = userId,
            concept = "Prueba CLP $index", unitAmountClp = 1500L, monthlyImpactClp = 6000L,
            weeklyFrequency = 1, expenseType = "VARIABLE", category = "TEST",
            paymentMethod = "CASH", isAntExpense = false, triggeredRuleCode = "",
            ruleReason = "", createdAt = System.currentTimeMillis()
        )

        // Exercise the lazy insert/transaction path once, separately from steady writes.
        val warmup = expense(-1)
        try {
            val firstWriteStart = SystemClock.elapsedRealtimeNanos()
            repository.insert(warmup)
            val firstWriteMs = (SystemClock.elapsedRealtimeNanos() - firstWriteStart) / 1_000_000.0
            println("FINANTZAS_FIRST_WRITE_MS=$firstWriteMs")
            assertNotNull(dao.getExpenseById(warmup.id, userId))
            assertNull(dao.getExpenseById(warmup.id, "other-$userId"))
        } finally { repository.delete(warmup) }

        repeat(20) { index ->
            val expense = expense(index)
            try {
                val start = SystemClock.elapsedRealtimeNanos()
                repository.insert(expense)
                val elapsedMs =
                    (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0

                timesMs.add(elapsedMs)

                assertNotNull(dao.getExpenseById(expense.id, userId))
                assertNull(dao.getExpenseById(expense.id, "other-$userId"))
            } finally { repository.delete(expense) }
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