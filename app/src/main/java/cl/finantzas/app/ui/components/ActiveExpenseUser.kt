package cl.finantzas.app.ui.components

import androidx.compose.runtime.*
import cl.finantzas.app.BuildConfig
import cl.finantzas.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth

const val LOCAL_EXPENSE_USER = "finantzas-demo-local"

@Composable
fun activeExpenseUser(): String {
    if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) return LOCAL_EXPENSE_USER
    val auth = remember { SupabaseProvider.client.auth }
    val session by auth.sessionStatus.collectAsState()
    return remember(session) { auth.currentUserOrNull()?.id ?: LOCAL_EXPENSE_USER }
}
