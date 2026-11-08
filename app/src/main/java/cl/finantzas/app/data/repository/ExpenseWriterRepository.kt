package cl.finantzas.app.data.repository

import cl.finantzas.app.data.local.entity.ExpenseEntityClp

interface ExpenseWriterRepository {

    suspend fun insert(expense: ExpenseEntityClp)

    suspend fun update(expense: ExpenseEntityClp)

    suspend fun delete(expense: ExpenseEntityClp)
}
