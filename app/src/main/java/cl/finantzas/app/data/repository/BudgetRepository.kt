package cl.finantzas.app.data.repository

import cl.finantzas.app.data.local.dao.BudgetLimitDao
import cl.finantzas.app.data.local.entity.BudgetLimitEntityClp
import cl.finantzas.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

val budgetDefaults = linkedMapOf(
    "GLOBAL" to ("Global" to 950_000L),
    "HOUSING_AND_UTILITIES" to ("Vivienda" to 400_000L),
    "FOOD_AND_GROCERIES" to ("Alimentación" to 280_000L),
    "TRANSPORT_AND_MOBILITY" to ("Transporte" to 110_000L),
    "MICRO_PURCHASES_AND_SNACKS" to ("Micro-compras" to 90_000L),
    "ENTERTAINMENT_SUBSCRIPTIONS" to ("Entretenimiento" to 25_000L)
)

@Serializable
data class BudgetRemoteDto(
    @SerialName("user_id") val userId: String,
    val category: String,
    @SerialName("monthly_limit_clp") val monthlyLimitClp: Long,
    @SerialName("updated_at") val updatedAt: String = java.time.Instant.now().toString()
)

class BudgetRepository(private val dao: BudgetLimitDao) {
    fun observe(userId: String) = dao.observe(userId)
    suspend fun initialize(userId: String) = dao.insertMissing(budgetDefaults.map { (category, value) ->
        BudgetLimitEntityClp(userId, category, value.second)
    })
    suspend fun save(userId: String, category: String, amount: Long) {
        require(category in budgetDefaults && amount > 0)
        initialize(userId)
        dao.edit(userId, category, amount)
    }
    suspend fun sync(userId: String) = syncMutex.withLock {
        val client = SupabaseProvider.client
        check(client.auth.currentUserOrNull()?.id == userId) { "Se requiere una sesión autenticada" }
        val remote = client.from("category_budgets").select {
            filter { eq("user_id", userId) }
        }.decodeList<BudgetRemoteDto>()
        remote.filter { it.userId == userId && it.monthlyLimitClp > 0 }.forEach {
            dao.insertMissing(listOf(BudgetLimitEntityClp(userId, it.category, it.monthlyLimitClp)))
            dao.acceptRemote(userId, it.category, it.monthlyLimitClp)
        }
        val existing = remote.map { it.category }.toSet()
        dao.snapshot(userId).filter { it.dirty || it.category !in existing }.forEach { row ->
            check(client.auth.currentUserOrNull()?.id == userId)
            client.from("category_budgets").upsert(BudgetRemoteDto(userId, row.category, row.monthlyLimitClp)) {
                onConflict = "user_id,category"
                ignoreDuplicates = !row.dirty
            }
            dao.acknowledge(userId, row.category, row.revision)
        }
    }
    companion object { private val syncMutex = Mutex() }
}
