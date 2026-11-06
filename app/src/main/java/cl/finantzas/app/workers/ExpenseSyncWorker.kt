package cl.finantzas.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Pendiente: sincronizar gastos con Supabase
        // cuando esté disponible la sesión autenticada.
        return Result.failure()
    }
}