package cl.finantzas.app.data.repository

import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.flow.Flow

interface ExpenseReaderRepository {

    fun getExpensesByUser(userId: String): Flow<List<ExpenseEntityClp>>

    suspend fun getExpenseById(
        expenseId: String,
        userId: String
    ): ExpenseEntityClp?

    fun getTotalMonthlyImpactClp(userId: String): Flow<Long>
}