package cl.finantzas.app.data.nfc

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcDemoTest {
    @Test
    fun `cada simulación respeta el monto y la frecuencia elegida`() {
        val amounts = listOf(3_200L, 12_990L, 24_500L)
        NfcDemoMerchant.entries.zip(amounts).forEach { (merchant, amount) ->
            val expense = createNfcDemoExpense(merchant, 4, "user", "id", 1L)
            val payload = expense.toNfcDemoPayload(merchant)
            assertEquals(amount, payload.amountClp)
            assertEquals(amount * 16, expense.monthlyImpactClp)
            assertEquals(4, payload.weeklyFrequency)
            assertEquals("NFC_MOBILE_WALLET", payload.paymentMethod)
            assertTrue(payload.simulated)
        }
        val monthly = createNfcDemoExpense(NfcDemoMerchant.COFFEE, 0, "user", "id", 1L)
        assertEquals(3_200L, monthly.monthlyImpactClp)
        assertFalse(monthly.isAntExpense)
        val weekly = createNfcDemoExpense(NfcDemoMerchant.COFFEE, 4, "user", "id", 1L)
        assertTrue(weekly.isAntExpense)
        assertEquals("RN-02", weekly.triggeredRuleCode)
        val gaming = createNfcDemoExpense(NfcDemoMerchant.GAMING, 0, "user", "id", 1L)
        assertEquals("RN-03", gaming.triggeredRuleCode)
    }

    @Test
    fun `la firma verifica el contenido original y rechaza cambios`() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val payload = "{\"simulated\":true,\"amount_clp\":3200}"
        val envelope = signNfcDemoPayload(payload, generator.generateKeyPair())
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(envelope.publicKeyBase64))
        )
        val signature = Base64.getDecoder().decode(envelope.signatureBase64)
        fun verify(content: String): Boolean = Signature.getInstance(envelope.algorithm).run {
            initVerify(publicKey)
            update(content.toByteArray(Charsets.UTF_8))
            verify(signature)
        }

        assertTrue(verify(payload))
        assertFalse(verify("{\"simulated\":true,\"amount_clp\":1}"))
    }
}
