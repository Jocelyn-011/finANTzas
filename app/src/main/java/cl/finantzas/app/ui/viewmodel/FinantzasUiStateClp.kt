package cl.finantzas.app.ui.viewmodel

import cl.aiep.finantzas.domain.rules.AntExpenseVerdict
import cl.aiep.finantzas.domain.rules.ExpenseCandidate

/**
 * Gasto ya evaluado por el motor de reglas, listo para mostrar en pantalla.
 * Todos los montos son CLP enteros (Long), sin decimales.
 */
data class EvaluatedExpenseClp(
    val candidate: ExpenseCandidate,
    val verdict: AntExpenseVerdict,
    val annualImpactClp: Long
)

/**
 * Estado inmutable de la pantalla principal de finANTzas.
 *
 * Cada cambio genera una copia nueva (copy) y nunca se modifica en el lugar,
 * asÃ­ la UI siempre recibe una "foto" consistente del estado (UDF).
 */
data class FinantzasUiStateClp(
    val expenses: List<EvaluatedExpenseClp> = emptyList(),
    val totalMonthlyImpactClp: Long = 0L,
    val totalAnnualImpactClp: Long = 0L,
    val antMonthlyImpactClp: Long = 0L,
    val antAnnualImpactClp: Long = 0L,
    val errorMessage: String? = null
)
