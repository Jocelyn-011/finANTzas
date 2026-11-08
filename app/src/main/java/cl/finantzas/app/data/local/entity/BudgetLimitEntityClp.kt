package cl.finantzas.app.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(tableName = "category_budgets", primaryKeys = ["user_id", "category"])
data class BudgetLimitEntityClp(
    @ColumnInfo(name = "user_id") val userId: String,
    val category: String,
    @ColumnInfo(name = "monthly_limit_clp") val monthlyLimitClp: Long,
    val revision: Long = 0,
    val dirty: Boolean = false
)
