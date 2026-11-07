package cl.finantzas.app.data.remote

import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class ExpenseRemoteRepository {

    private val client = SupabaseProvider.client

    suspend fun uploadExpenses(expenses: List<ExpenseEntityClp>): Int {
        val user = client.auth.currentUserOrNull()
            ?: throw IllegalStateException("No hay usuario autenticado")

        val ownExpenses = expenses.filter { it.userId == user.id }

        if (ownExpenses.isEmpty()) return 0

        client.from("expenses").upsert(
            ownExpenses.map { it.toRemoteDto() }
        )

        return ownExpenses.size
    }
}