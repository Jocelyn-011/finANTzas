package cl.finantzas.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.remote.ExpenseRemoteRepository
import cl.finantzas.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import java.io.IOException

class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val client = SupabaseProvider.client

        // Sin sesiÃ³n autenticada no se deben enviar datos.
        val user = client.auth.currentUserOrNull()
            ?: return Result.success()

        return try {
            val database = SecureDatabaseProvider.getDatabase(applicationContext)

            val expenses = database.expenseDao()
                .getExpensesForSync(user.id)

            if (expenses.isEmpty()) {
                return Result.success()
            }

            ExpenseRemoteRepository().uploadExpenses(expenses)

            Result.success()

        } catch (e: IOException) {
            Result.retry()

        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}