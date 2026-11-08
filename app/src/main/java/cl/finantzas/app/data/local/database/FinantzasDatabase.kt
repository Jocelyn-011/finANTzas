package cl.finantzas.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.finantzas.app.data.local.dao.ExpenseDao
import cl.finantzas.app.data.local.entity.ExpenseEntityClp

@Database(
    entities = [ExpenseEntityClp::class, cl.finantzas.app.data.local.entity.BudgetLimitEntityClp::class],
    version = 2,
    exportSchema = false
)
abstract class FinantzasDatabase : RoomDatabase() {
    abstract fun budgetLimitDao(): cl.finantzas.app.data.local.dao.BudgetLimitDao
    abstract fun expenseDao(): ExpenseDao
}
