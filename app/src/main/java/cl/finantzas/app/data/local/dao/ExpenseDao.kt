package cl.finantzas.app.data.local.dao

import androidx.room.*
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntityClp)

    @Update
    suspend fun update(expense: ExpenseEntityClp)

    @Delete
    suspend fun delete(expense: ExpenseEntityClp)

    @Query("SELECT * FROM expenses WHERE user_id = :userId ORDER BY created_at DESC")
    fun getExpensesByUser(userId: String): Flow<List<ExpenseEntityClp>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId AND user_id = :userId LIMIT 1")
    suspend fun getExpenseById(expenseId: String, userId: String): ExpenseEntityClp?

    @Query("SELECT COALESCE(SUM(monthly_impact_clp), 0) FROM expenses WHERE user_id = :userId")
    fun getTotalMonthlyImpactClp(userId: String): Flow<Long>
}
