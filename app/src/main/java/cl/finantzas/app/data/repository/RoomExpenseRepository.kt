package cl.finantzas.app.data.repository

import cl.finantzas.app.data.local.dao.ExpenseDao
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomExpenseRepository(
    private val expenseDao: ExpenseDao
) : ExpenseReaderRepository, ExpenseWriterRepository {

    override fun getExpensesByUser(
        userId: String
    ): Flow<List<ExpenseEntityClp>> =
        expenseDao.getExpensesByUser(userId).distinctUntilChanged()

    override suspend fun getExpenseById(
        expenseId: String,
        userId: String
    ): ExpenseEntityClp? =
        expenseDao.getExpenseById(expenseId, userId)

    override fun getTotalMonthlyImpactClp(
        userId: String
    ): Flow<Long> =
        expenseDao.getTotalMonthlyImpactClp(userId).distinctUntilChanged()

    override suspend fun insert(expense: ExpenseEntityClp) {
        require(expense.unitAmountClp > 0L)
        require(expense.monthlyImpactClp > 0L)
        expenseDao.insert(expense)
    }

    override suspend fun update(expense: ExpenseEntityClp) {
        require(expense.unitAmountClp > 0L)
        require(expense.monthlyImpactClp > 0L)
        expenseDao.update(expense)
    }

    override suspend fun delete(expense: ExpenseEntityClp) {
        expenseDao.delete(expense)
    }
}
