package cl.finantzas.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.finantzas.app.data.local.dao.ExpenseDao
import cl.finantzas.app.data.local.entity.ExpenseEntityClp

@Database(
    entities = [ExpenseEntityClp::class],
    version = 1,
    exportSchema = false
)
abstract class FinantzasDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}
