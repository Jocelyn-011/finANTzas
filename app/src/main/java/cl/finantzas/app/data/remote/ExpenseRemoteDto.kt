package cl.finantzas.app.data.remote

import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ExpenseRemoteDto(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    val concept: String,

    @SerialName("unit_amount_clp")
    val unitAmountClp: Long,

    @SerialName("weekly_frequency")
    val weeklyFrequency: Int,

    @SerialName("expense_type")
    val expenseType: String,

    val category: String,

    @SerialName("payment_method")
    val paymentMethod: String,

    @SerialName("is_ant_expense")
    val isAntExpense: Boolean,

    @SerialName("triggered_rule_code")
    val triggeredRuleCode: String,

    @SerialName("rule_reason")
    val ruleReason: String,

    @SerialName("created_at")
    val createdAt: String
)

fun ExpenseEntityClp.toRemoteDto() = ExpenseRemoteDto(
    id = id,
    userId = userId,
    concept = concept,
    unitAmountClp = unitAmountClp,
    weeklyFrequency = weeklyFrequency,
    expenseType = expenseType,
    category = category,
    paymentMethod = paymentMethod,
    isAntExpense = isAntExpense,
    triggeredRuleCode = triggeredRuleCode,
    ruleReason = ruleReason,
    createdAt = Instant.ofEpochMilli(createdAt).toString()
)