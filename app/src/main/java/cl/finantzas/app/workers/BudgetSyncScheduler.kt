package cl.finantzas.app.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import cl.finantzas.app.BuildConfig
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.remote.SupabaseProvider
import cl.finantzas.app.data.repository.BudgetRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException

object BudgetSyncScheduler {
    fun schedule(context: Context) {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) return
        WorkManager.getInstance(context).enqueueUniqueWork("budget_sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<BudgetSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
}
class BudgetSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val auth = SupabaseProvider.client.auth
            auth.awaitInitialization()
            val user = auth.currentUserOrNull() ?: return Result.success()
            BudgetRepository(SecureDatabaseProvider.getDatabase(applicationContext).budgetLimitDao()).sync(user.id)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }
}
