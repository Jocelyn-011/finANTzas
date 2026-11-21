package cl.finantzas.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Transaction
import androidx.room.OnConflictStrategy
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntityClp)

    @Query("SELECT user_id FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseOwner(expenseId: String): String?

    @Transaction
    suspend fun insertForOwner(expense: ExpenseEntityClp) {
        val owner = getExpenseOwner(expense.id)
        require(owner == null || owner == expense.userId)
        insert(expense)
    }

    @Transaction
    suspend fun updateForOwner(expense: ExpenseEntityClp) {
        require(getExpenseOwner(expense.id) == expense.userId)
        update(expense)
    }

    @Transaction
    suspend fun deleteForOwner(expense: ExpenseEntityClp) {
        require(getExpenseOwner(expense.id) == expense.userId)
        delete(expense)
    }

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

    @Query("DELETE FROM expenses WHERE user_id = :userId AND is_demo = 1")
    suspend fun deleteDemoExpenses(userId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDemoExpenses(expenses: List<ExpenseEntityClp>)

    @Transaction
    suspend fun restoreDemoExpenses(userId: String, demoExpenses: List<ExpenseEntityClp>) {
        require(userId.isNotBlank())
        require(demoExpenses.all { it.userId == userId && it.isDemo })
        deleteDemoExpenses(userId)
        insertDemoExpenses(demoExpenses)
    }
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND is_demo = 0 ORDER BY created_at ASC")
    suspend fun getExpensesForSync(userId: String): List<ExpenseEntityClp>
}
