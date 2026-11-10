package cl.finantzas.app.data.nfc

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyFactory
import java.security.KeyStore
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NfcDemoSignerInstrumentedTest {
    @Test
    fun firmaDeDemostracionSeVerificaConClavePublicaYProtegeClavePrivada() {
        val payload = NfcDemoPayloadClp(
            eventId = "test-event",
            createdAt = 1L,
            merchant = "Comercio de prueba",
            amountClp = 3_200L,
            weeklyFrequency = 4,
            monthlyImpactClp = 51_200L,
            category = "MICRO_PURCHASES_AND_SNACKS",
            expenseType = "PURCHASE",
            paymentMethod = "NFC_MOBILE_WALLET",
            isAntExpense = true,
            ruleCode = "RN-02"
        )
        val envelope = NfcDemoSigner().sign(payload)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(envelope.publicKeyBase64))
        )
        val valid = Signature.getInstance(envelope.algorithm).run {
            initVerify(publicKey)
            update(envelope.payloadJson.toByteArray(Charsets.UTF_8))
            verify(Base64.getDecoder().decode(envelope.signatureBase64))
        }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = store.getKey("finantzas.nfc.simulation.signing.v1", null)

        assertTrue(valid)
        assertTrue(envelope.payloadJson.contains("\"simulated\":true"))
        assertNotNull(privateKey)
        assertNull(privateKey.encoded)
    }
}
