package cl.aiep.finantzas.domain.rules

/**
 * Resultado inmutable del análisis de fuga por medio de pago (RN-04).
 * Todos los montos se expresan estrictamente en CLP como enteros [Long].
 */
data class PaymentMethodLeakageResult(
    val instrument: PaymentInstrument,
    val totalExpenseAmountClp: Long,
    val antExpenseAmountClp: Long,
    val leakagePercentage: Int
)

/**
 * Calculador de analítica financiera y reglas de fuga (RN-04).
 *
 * Cumple con el Principio de Sustitución de Liskov (LSP): procesa cualquier [PaymentInstrument]
 * a través de su abstracción polimórfica sin dependencias hacia implementaciones concretas.
 */
object FinancialAnalyticsCalculator {

    const val PERCENTAGE_MULTIPLIER: Long = 100L
    private const val ZERO_PERCENTAGE: Int = 0
    private const val ZERO_AMOUNT_CLP: Long = 0L

    /**
     * Regla RN-04: Porcentaje de Fuga por Medio de Pago (Principio LSP).
     *
     * Fórmula: ((antExpenseAmountClp * 100L) / totalExpenseAmountClp).toInt()
     *
     * Guardia contra División por Cero: Si [totalExpenseAmountClp] es 0L o negativo,
     * retorna inmediatamente 0% de fuga y 0L de fuga en CLP evitando [ArithmeticException].
     */
    fun calculatePaymentMethodLeakage(
        instrument: PaymentInstrument,
        totalExpenseAmountClp: Long,
        antExpenseAmountClp: Long
    ): PaymentMethodLeakageResult {
        if (totalExpenseAmountClp <= ZERO_AMOUNT_CLP) {
            return PaymentMethodLeakageResult(
                instrument = instrument,
                totalExpenseAmountClp = ZERO_AMOUNT_CLP,
                antExpenseAmountClp = ZERO_AMOUNT_CLP,
                leakagePercentage = ZERO_PERCENTAGE
            )
        }

        val percentage = ((antExpenseAmountClp * PERCENTAGE_MULTIPLIER) / totalExpenseAmountClp).toInt()

        return PaymentMethodLeakageResult(
            instrument = instrument,
            totalExpenseAmountClp = totalExpenseAmountClp,
            antExpenseAmountClp = antExpenseAmountClp,
            leakagePercentage = percentage
        )
    }
}