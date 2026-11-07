package cl.aiep.finantzas.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClpImpactCalculatorTest {

    @Test
    fun `micro-compra de 3500 CLP 5 veces por semana da 70000 mensual y 840000 anual`() {
        val monthly = ClpImpactCalculator.monthlyImpactClp(unitAmountClp = 3_500L, weeklyFrequency = 5)

        assertEquals(70_000L, monthly)                                    // 3.500 * 5 * 4
        assertEquals(840_000L, ClpImpactCalculator.annualImpactClp(monthly)) // 70.000 * 12
    }

    @Test
    fun `suscripcion mensual fija de 26900 CLP da 26900 mensual y 322800 anual`() {
        val monthly = ClpImpactCalculator.monthlyImpactClp(
            unitAmountClp = 26_900L,
            weeklyFrequency = FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE
        )

        assertEquals(26_900L, monthly)
        assertEquals(322_800L, ClpImpactCalculator.annualImpactClp(monthly))
    }

    @Test
    fun `sumClp suma montos CLP de forma exacta`() {
        assertEquals(96_900L, ClpImpactCalculator.sumClp(listOf(70_000L, 26_900L)))
        assertEquals(0L, ClpImpactCalculator.sumClp(emptyList()))
    }

    @Test
    fun `el motor de reglas usa exactamente el mismo calculo mensual`() {
        val candidate = ExpenseCandidate(
            concept = "Café",
            unitAmountClp = 3_500L,
            weeklyFrequency = 5,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS,
            paymentMethodCode = "DEBIT_CARD"
        )

        val verdict = AntExpenseRuleEngine().evaluate(candidate)

        assertEquals(ClpImpactCalculator.monthlyImpactClp(3_500L, 5), verdict.monthlyImpactClp)
    }

    @Test
    fun `desbordamiento de Long lanza ArithmeticException en vez de un monto corrupto`() {
        assertThrows(ArithmeticException::class.java) {
            ClpImpactCalculator.annualImpactClp(Long.MAX_VALUE)
        }
        assertThrows(ArithmeticException::class.java) {
            ClpImpactCalculator.sumClp(listOf(Long.MAX_VALUE, 1L))
        }
    }

    @Test
    fun `monto o frecuencia negativos son rechazados`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClpImpactCalculator.monthlyImpactClp(unitAmountClp = -1L, weeklyFrequency = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ClpImpactCalculator.monthlyImpactClp(unitAmountClp = 1_000L, weeklyFrequency = -1)
        }
    }
}
