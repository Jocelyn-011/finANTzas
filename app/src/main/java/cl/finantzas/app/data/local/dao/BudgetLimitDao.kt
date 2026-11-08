package cl.finantzas.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import cl.finantzas.app.data.local.entity.BudgetLimitEntityClp
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM category_budgets WHERE user_id = :userId")
    fun observe(userId: String): Flow<List<BudgetLimitEntityClp>>
    @Query("SELECT * FROM category_budgets WHERE user_id = :userId")
    suspend fun snapshot(userId: String): List<BudgetLimitEntityClp>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(rows: List<BudgetLimitEntityClp>)
    @Query("""
        UPDATE category_budgets
        SET monthly_limit_clp = :amount, revision = revision + 1, dirty = 1
        WHERE user_id = :userId AND category = :category
    """)
    suspend fun edit(userId: String, category: String, amount: Long)
    @Query("""
        UPDATE category_budgets
        SET monthly_limit_clp = :amount
        WHERE user_id = :userId AND category = :category AND dirty = 0
    """)
    suspend fun acceptRemote(userId: String, category: String, amount: Long)
    @Query("""
        UPDATE category_budgets
        SET dirty = 0
        WHERE user_id = :userId AND category = :category AND revision = :revision
    """)
    suspend fun acknowledge(userId: String, category: String, revision: Long)
}
