package cl.finantzas.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.remote.ExpenseRemoteRepository
import cl.finantzas.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import java.io.IOException
import kotlinx.coroutines.CancellationException

class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object { const val MAX_RETRIES = 5 }

    override suspend fun doWork(): Result {
        val client = SupabaseProvider.client

        // Sin sesiÃ³n autenticada no se deben enviar datos.
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull()
            ?: return Result.success()

        return try {
            val database = SecureDatabaseProvider.getDatabase(applicationContext)

            val expenses = database.expenseDao()
                .getExpensesForSync(user.id)

            if (expenses.isNotEmpty()) {
                ExpenseRemoteRepository().uploadExpenses(expenses)
            }

            Result.success()

        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Result.retry()

        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }
}
