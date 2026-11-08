package cl.aiep.finantzas.domain.rules

/**
 * Única fuente de verdad para los cálculos de impacto en CLP (Long, sin decimales).
 *
 * Usa aritmética exacta (Math.multiplyExact / Math.addExact): si un cálculo
 * desborda Long, lanza ArithmeticException en lugar de devolver un monto
 * corrupto (Integridad - Tríada CID).
 */
object ClpImpactCalculator {

    private const val SINGLE_MONTHLY_OCCURRENCE: Long = 1L
    private const val MIN_VALID_AMOUNT_CLP: Long = 0L

    /**
     * Impacto mensual = unitAmountClp * (1L si es mensual fijo, si no weeklyFrequency * 4L).
     */
    fun monthlyImpactClp(unitAmountClp: Long, weeklyFrequency: Int): Long {
        require(unitAmountClp >= MIN_VALID_AMOUNT_CLP) { "El monto en CLP no puede ser negativo" }
        require(weeklyFrequency >= FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE) {
            "La frecuencia semanal no puede ser negativa"
        }

        val monthlyOccurrences =
            if (weeklyFrequency == FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE) {
                SINGLE_MONTHLY_OCCURRENCE
            } else {
                Math.multiplyExact(weeklyFrequency.toLong(), FinancialThresholds.WEEKS_PER_MONTH_MULTIPLIER)
            }

        return Math.multiplyExact(unitAmountClp, monthlyOccurrences)
    }

    /** Impacto anual = impacto mensual * 12L. */
    fun annualImpactClp(monthlyImpactClp: Long): Long =
        Math.multiplyExact(monthlyImpactClp, FinancialThresholds.MONTHS_IN_YEAR.toLong())

    /** Suma exacta de montos CLP (reemplaza sumOf, que desborda en silencio). */
    fun sumClp(amountsClp: Iterable<Long>): Long =
        amountsClp.fold(0L) { total, amount -> Math.addExact(total, amount) }
}
