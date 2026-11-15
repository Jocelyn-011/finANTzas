package cl.finantzas.app.data.repository

import cl.finantzas.app.data.local.entity.ExpenseEntityClp

interface ExpenseWriterRepository {

    suspend fun restoreDemoExpenses(userId: String) {
        error("Restauraci?n no disponible")
    }

    suspend fun insert(expense: ExpenseEntityClp)

    suspend fun update(expense: ExpenseEntityClp)

    suspend fun delete(expense: ExpenseEntityClp)
}
