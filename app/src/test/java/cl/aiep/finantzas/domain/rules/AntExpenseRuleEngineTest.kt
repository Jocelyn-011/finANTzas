package cl.aiep.finantzas.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ============================================================================
 * ARCHIVO PARA ADJUNTAR EN CLICKUP: Tarea T1.3 (Álvaro Brizuela)
 * Ubicación en Android Studio: app/src/test/java/cl/aiep/finantzas/domain/rules/AntExpenseRuleEngineTest.kt
 * Herramientas: JUnit 4 + Android Studio Test Runner + Cobertura JaCoCo + Detekt
 * ============================================================================
 */
class AntExpenseRuleEngineTest {

    private val engine = AntExpenseRuleEngine()

    // ------------------------------------------------------------------------
    // 1. Verificación de Constantes Clean Code (object FinancialThresholds) y Formato CLP
    // ------------------------------------------------------------------------
    @Test
    fun `debe verificar que las constantes en FinancialThresholds cumplan las reglas de negocio en CLP`() {
        assertEquals(15_000L, FinancialThresholds.DEFAULT_MICRO_TX_MAX_AMOUNT_CLP)
        assertEquals(3, FinancialThresholds.MAX_WEEKLY_MICRO_TX_FREQUENCY)
        assertEquals(0, FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE)
        assertEquals(4L, FinancialThresholds.WEEKS_PER_MONTH_MULTIPLIER)
        assertEquals(12, FinancialThresholds.MONTHS_IN_YEAR)
        assertEquals(75, FinancialThresholds.BUDGET_WARNING_PERCENTAGE)
        assertEquals(100, FinancialThresholds.BUDGET_CRITICAL_PERCENTAGE)
    }

    @Test
    fun `ClpCurrencyFormatter debe formatear montos enteros en CLP con punto de miles y sin decimales`() {
        assertEquals("$3.500", ClpCurrencyFormatter.formatClp(3_500L))
        assertEquals("$15.000", ClpCurrencyFormatter.formatClp(15_000L))
        assertEquals("$70.000", ClpCurrencyFormatter.formatClp(70_000L))
        assertEquals("$950.000", ClpCurrencyFormatter.formatClp(950_000L))
    }

    // ------------------------------------------------------------------------
    // 2. Validación de Reglas RN-02 (>3/sem) y RN-03 (Entretenimiento) en CLP
    // ------------------------------------------------------------------------
    @Test
    fun `debe clasificar como Gasto Hormiga RN-02 cuando micro-compra de 3500 CLP se repite 5 veces por semana dando 70000 CLP al mes`() {
        // Arrange (Preparar)
        val candidate = ExpenseCandidate(
            concept = "Café de grano camino a clases",
            unitAmountClp = 3_500L, // $3.500 CLP
            weeklyFrequency = 5,    // 5 veces por semana (> 3/sem)
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.MICRO_PURCHASES_AND_SNACKS,
            paymentMethodCode = "CREDIT_CARD"
        )

        // Act (Actuar)
        val verdict = engine.evaluate(candidate)

        // Assert (Afirmar)
        assertTrue(verdict.isAntExpense)
        assertEquals("RN-02", verdict.triggeredRuleCode)
        assertEquals(70_000L, verdict.monthlyImpactClp) // $3.500 * 5 * 4 = $70.000 CLP/mes
    }

    @Test
    fun `NO debe clasificar como Gasto Hormiga si la micro-compra se repite 3 veces o menos por semana`() {
        val candidate = ExpenseCandidate(
            concept = "Colación ocasional",
            unitAmountClp = 3_500L,
            weeklyFrequency = 3, // Exactamente 3 veces/semana (no supera el umbral > 3)
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.FOOD_AND_GROCERIES,
            paymentMethodCode = "DEBIT_CARD"
        )

        val verdict = engine.evaluate(candidate)

        assertFalse(verdict.isAntExpense)
        assertEquals("ESSENTIAL", verdict.triggeredRuleCode)
        assertEquals(42_000L, verdict.monthlyImpactClp) // $3.500 * 3 * 4 = $42.000 CLP/mes
    }

    @Test
    fun `debe clasificar como Gasto Hormiga RN-03 cualquier suscripcion de entretenimiento en CLP`() {
        val candidate = ExpenseCandidate(
            concept = "Combo Streaming Video y Música",
            unitAmountClp = 26_900L, // $26.900 CLP al mes
            weeklyFrequency = FinancialThresholds.MONTHLY_FIXED_FREQUENCY_CODE, // 0 = Mensual fijo
            type = ExpenseType.SUBSCRIPTION,
            category = ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS,
            paymentMethodCode = "CREDIT_CARD"
        )

        val verdict = engine.evaluate(candidate)

        assertTrue(verdict.isAntExpense)
        assertEquals("RN-03", verdict.triggeredRuleCode)
        assertEquals(26_900L, verdict.monthlyImpactClp)
    }

    // ------------------------------------------------------------------------
    // 3. Validación del Patrón Strategy (Principio SOLID OCP con fun interface)
    // ------------------------------------------------------------------------
    @Test
    fun `debe permitir inyectar una nueva regla personalizada mediante fun interface AntExpenseRule sin modificar el motor (OCP)`() {
        // Arrange: Creamos una regla nueva al vuelo usando lambda sobre la fun interface AntExpenseRule
        val customDeliveryWeekendRule = AntExpenseRule { candidate ->
            if (candidate.concept.contains("Delivery", ignoreCase = true)) {
                Triple(true, "RN-CUSTOM", "Regla personalizada de prueba: Delivery")
            } else {
                null
            }
        }

        val customEngine = AntExpenseRuleEngine(
            rules = listOf(
                MicroTransactionFrequencyRule(),
                EntertainmentSubscriptionRule(),
                customDeliveryWeekendRule
            )
        )

        val candidate = ExpenseCandidate(
            concept = "Pedido Delivery nocturno",
            unitAmountClp = 18_000L,
            weeklyFrequency = 1,
            type = ExpenseType.PURCHASE,
            category = ExpenseCategory.FOOD_AND_GROCERIES,
            paymentMethodCode = "NFC_MOBILE_WALLET"
        )

        // Act
        val verdict = customEngine.evaluate(candidate)

        // Assert
        assertTrue(verdict.isAntExpense)
        assertEquals("RN-CUSTOM", verdict.triggeredRuleCode)
        assertEquals(72_000L, verdict.monthlyImpactClp)
    }
}
