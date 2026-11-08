package cl.aiep.finantzas.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAnalyticsCalculatorTest {

    @Test
    fun `caso borde division por cero cuando gasto total es 0L retorna 0 porciento y 0L de fuga sin excepcion`() {
        val instrument = PaymentInstrument.CreditCard

        val result = FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
            instrument = instrument,
            totalExpenseAmountClp = 0L,
            antExpenseAmountClp = 0L
        )

        assertEquals(0, result.leakagePercentage)
        assertEquals(0L, result.antExpenseAmountClp)
        assertEquals(0L, result.totalExpenseAmountClp)
        assertEquals(instrument, result.instrument)
    }

    @Test
    fun `calcula porcentaje entero de fuga exacto segun formula RN-04 en CLP`() {
        // Ejemplo: 70.000 CLP de fuga sobre 185.000 CLP total = (70000 * 100) / 185000 = 37%
        val result = FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
            instrument = PaymentInstrument.CreditCard,
            totalExpenseAmountClp = 185_000L,
            antExpenseAmountClp = 70_000L
        )

        assertEquals(37, result.leakagePercentage)
        assertEquals(70_000L, result.antExpenseAmountClp)
        assertEquals(185_000L, result.totalExpenseAmountClp)
    }

    @Test
    fun `cumple principio LSP procesando polimorficamente subtipos de PaymentInstrument y ordenando ranking descendentemente`() {
        // 1. Tarjeta de Crédito: 100% de fuga (todo gasto hormiga)
        val creditLeak = FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
            instrument = PaymentInstrument.CreditCard,
            totalExpenseAmountClp = 100_000L,
            antExpenseAmountClp = 100_000L
        )

        // 2. Billetera NFC: 50% de fuga
        val nfcLeak = FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
            instrument = PaymentInstrument.NfcMobileWallet,
            totalExpenseAmountClp = 44_000L,
            antExpenseAmountClp = 22_000L
        )

        // 3. Tarjeta de Débito: 0% de fuga (gasto 100% esencial)
        val debitLeak = FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
            instrument = PaymentInstrument.DebitCard,
            totalExpenseAmountClp = 260_000L,
            antExpenseAmountClp = 0L
        )

        // 4. Efectivo: 25% de fuga
        val cashLeak = FinancialAnalyticsCalculator.calculatePaymentMethodLeakage(
            instrument = PaymentInstrument.Cash,
            totalExpenseAmountClp = 40_000L,
            antExpenseAmountClp = 10_000L
        )

        val unrankedList = listOf(debitLeak, creditLeak, cashLeak, nfcLeak)
        val rankedList = unrankedList.sortedByDescending { it.leakagePercentage }

        // Verificación de sustitución de Liskov y orden de ranking
        assertEquals(4, rankedList.size)
        assertEquals(PaymentInstrument.CreditCard, rankedList[0].instrument)
        assertEquals(100, rankedList[0].leakagePercentage)

        assertEquals(PaymentInstrument.NfcMobileWallet, rankedList[1].instrument)
        assertEquals(50, rankedList[1].leakagePercentage)

        assertEquals(PaymentInstrument.Cash, rankedList[2].instrument)
        assertEquals(25, rankedList[2].leakagePercentage)

        assertEquals(PaymentInstrument.DebitCard, rankedList[3].instrument)
        assertEquals(0, rankedList[3].leakagePercentage)

        assertTrue(rankedList[0].leakagePercentage >= rankedList[1].leakagePercentage)
        assertTrue(rankedList[1].leakagePercentage >= rankedList[2].leakagePercentage)
        assertTrue(rankedList[2].leakagePercentage >= rankedList[3].leakagePercentage)
    }
}