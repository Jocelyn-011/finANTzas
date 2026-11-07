package cl.aiep.finantzas.domain.rules

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Constantes de umbrales financieros y reglas de negocio en CLP.
 */
object FinancialThresholds {
    const val DEFAULT_MICRO_TX_MAX_AMOUNT_CLP: Long = 15_000L
    const val MAX_WEEKLY_MICRO_TX_FREQUENCY: Int = 3
    const val MONTHLY_FIXED_FREQUENCY_CODE: Int = 0
    const val WEEKS_PER_MONTH_MULTIPLIER: Long = 4L
    const val MONTHS_IN_YEAR: Int = 12
    const val BUDGET_WARNING_PERCENTAGE: Int = 75
    const val BUDGET_CRITICAL_PERCENTAGE: Int = 100
}

/**
 * Formateador de moneda en CLP (Pesos Chilenos).
 */
object ClpCurrencyFormatter {
    fun formatClp(amount: Long): String {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("es-CL")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return "$${formatter.format(amount)}"
    }
}

/**
 * Tipo de gasto (Compra directa o Suscripción).
 */
enum class ExpenseType {
    PURCHASE,
    SUBSCRIPTION
}

/**
 * Categorías de gastos evaluadas por el motor de reglas.
 */
enum class ExpenseCategory {
    MICRO_PURCHASES_AND_SNACKS,
    FOOD_AND_GROCERIES,
    ENTERTAINMENT_SUBSCRIPTIONS
}

/**
 * Candidato de gasto a evaluar.
 */
data class ExpenseCandidate(
    val concept: String,
    val unitAmountClp: Long,
    val weeklyFrequency: Int,
    val type: ExpenseType,
    val category: ExpenseCategory,
    val paymentMethodCode: String
)

/**
 * Resultado de la evaluación del motor de reglas.
 */
data class AntExpenseVerdict(
    val isAntExpense: Boolean,
    val triggeredRuleCode: String,
    val monthlyImpactClp: Long,
    val description: String? = null
)

/**
 * Interfaz funcional para implementar reglas de gastos hormiga (Patrón Strategy - OCP).
 */
fun interface AntExpenseRule {
    fun evaluate(candidate: ExpenseCandidate): Triple<Boolean, String, String>?
}

/**
 * Regla RN-02: Micro-compras frecuentes (>3 veces por semana).
 */
class MicroTransactionFrequencyRule : AntExpenseRule {
    override fun evaluate(candidate: ExpenseCandidate): Triple<Boolean, String, String>? {
        if (candidate.type == ExpenseType.PURCHASE &&
            candidate.weeklyFrequency > FinancialThresholds.MAX_WEEKLY_MICRO_TX_FREQUENCY
        ) {
            return Triple(
                true,
                "RN-02",
                "Micro-compra frecuente que supera el límite semanal de ${FinancialThresholds.MAX_WEEKLY_MICRO_TX_FREQUENCY} veces"
            )
        }
        return null
    }
}

/**
 * Regla RN-03: Suscripciones de entretenimiento.
 */
class EntertainmentSubscriptionRule : AntExpenseRule {
    override fun evaluate(candidate: ExpenseCandidate): Triple<Boolean, String, String>? {
        if (candidate.category == ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS ||
            candidate.type == ExpenseType.SUBSCRIPTION
        ) {
            return Triple(
                true,
                "RN-03",
                "Suscripción de entretenimiento clasificada como gasto hormiga"
            )
        }
        return null
    }
}

/**
 * Motor evaluador de reglas para detección de Gastos Hormiga.
 */
class AntExpenseRuleEngine(
    private val rules: List<AntExpenseRule> = listOf(
        MicroTransactionFrequencyRule(),
        EntertainmentSubscriptionRule()
    )
) {
    fun evaluate(candidate: ExpenseCandidate): AntExpenseVerdict {
        val monthlyImpactClp = if (candidate.weeklyFrequency == FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE) {
            candidate.unitAmountClp
        } else {
            candidate.unitAmountClp * candidate.weeklyFrequency * FinancialThresholds.WEEKS_PER_MONTH_MULTIPLIER
        }

        for (rule in rules) {
            val result = rule.evaluate(candidate)
            if (result != null && result.first) {
                return AntExpenseVerdict(
                    isAntExpense = true,
                    triggeredRuleCode = result.second,
                    monthlyImpactClp = monthlyImpactClp,
                    description = result.third
                )
            }
        }

        return AntExpenseVerdict(
            isAntExpense = false,
            triggeredRuleCode = "ESSENTIAL",
            monthlyImpactClp = monthlyImpactClp,
            description = "Gasto esencial u ordinario no clasificado como gasto hormiga"
        )
    }
}
