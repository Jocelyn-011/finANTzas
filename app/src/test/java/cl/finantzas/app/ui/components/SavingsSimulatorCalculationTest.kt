package cl.finantzas.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SavingsSimulatorCalculationTest {
    @Test
    fun `calcula los tres escenarios en pesos enteros`() {
        assertEquals(47_097L, savingsPerMonth(188_390L, 25))
        assertEquals(94_195L, savingsPerMonth(188_390L, 50))
        assertEquals(188_390L, savingsPerMonth(188_390L, 100))
    }

    @Test
    fun `trunca fracciones de peso y admite cero`() {
        assertEquals(0L, savingsPerMonth(3L, 25))
        assertEquals(0L, savingsPerMonth(0L, 100))
    }

    @Test
    fun `calcula sin desbordar para un monto anual representable`() {
        val monthlyBase = Long.MAX_VALUE / 12
        assertEquals(monthlyBase, savingsPerMonth(monthlyBase, 100))
        assertEquals(monthlyBase / 2, savingsPerMonth(monthlyBase, 50))
    }
}
