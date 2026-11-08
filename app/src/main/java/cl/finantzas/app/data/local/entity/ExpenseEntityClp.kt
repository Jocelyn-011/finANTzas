package cl.finantzas.app.data.local.entity

import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.ExpenseType
import cl.aiep.finantzas.domain.rules.PaymentInstrument

/**
 * Entidad inmutable para representación o persistencia local de un gasto.
 * Todos los montos monetarios son estrictamente en CLP como enteros [Long].
 */
data class ExpenseEntityClp(
    val id: Long,
    val concept: String,
    val unitAmountClp: Long,
    val weeklyFrequency: Int,
    val type: ExpenseType,
    val category: ExpenseCategory,
    val paymentInstrument: PaymentInstrument,
    val isAntExpense: Boolean = false,
    val ruleReason: String? = null
)