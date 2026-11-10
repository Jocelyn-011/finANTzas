package cl.aiep.finantzas.nfc

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NfcContactlessSecurityFlowTest {

    private val testSecretKey32Bytes = "ClavePruebaHmac32BytesFinantzas!".toByteArray(StandardCharsets.UTF_8)

    @Before
    fun setUp() {
        HostApduServiceNfc.clearAuthorizedPayload()
        NfcTransactionSyncSecurityGuard.clearProcessedIdempotencyKeys()
    }

    @Test
    fun `flujo completo NFC HCE y HmacSHA256 autoriza y transmite pagos enteros en CLP sin decimales`() {
        val amountsClp = listOf(3_200L, 12_990L, 24_500L)

        amountsClp.forEach { amountClp ->
            val signedPayload = NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
                amountClp = amountClp,
                merchantName = "Comercio Chile",
                secretKeyBytes = testSecretKey32Bytes
            )

            assertTrue(
                NfcTransactionSyncSecurityGuard.verifyPayloadIntegrity(signedPayload, testSecretKey32Bytes)
            )
            assertFalse(signedPayload.canonicalMessage.contains("."))
            assertFalse(signedPayload.canonicalMessage.contains(","))

            HostApduServiceNfc.armAuthorizedPayload(signedPayload.toApduUtf8Bytes())
            val selectAidCommand = HostApduServiceNfc.buildSelectAidCommandApdu()
            val apduResponse = HostApduServiceNfc.evaluateApduCommand(selectAidCommand)

            val statusWord = apduResponse.copyOfRange(apduResponse.size - 2, apduResponse.size)
            val transmittedBytes = apduResponse.copyOfRange(0, apduResponse.size - 2)

            assertArrayEquals(HostApduServiceNfc.SW_OK, statusWord)
            assertEquals(
                "${signedPayload.canonicalMessage}|${signedPayload.hmacSha256Hex}",
                String(transmittedBytes, StandardCharsets.UTF_8)
            )
        }
    }

    @Test
    fun `token NFC HCE es de un solo uso y rechaza un segundo toque sin nueva biometria`() {
        val signedPayload = NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
            amountClp = 12_990L,
            merchantName = "Almuerzo Ejecutivo",
            secretKeyBytes = testSecretKey32Bytes
        )
        HostApduServiceNfc.armAuthorizedPayload(signedPayload.toApduUtf8Bytes())
        val selectAidCommand = HostApduServiceNfc.buildSelectAidCommandApdu()

        val firstTapResponse = HostApduServiceNfc.evaluateApduCommand(selectAidCommand)
        val secondTapResponse = HostApduServiceNfc.evaluateApduCommand(selectAidCommand)

        val firstSw = firstTapResponse.copyOfRange(firstTapResponse.size - 2, firstTapResponse.size)
        assertArrayEquals(HostApduServiceNfc.SW_OK, firstSw)
        assertArrayEquals(HostApduServiceNfc.SW_CONDITIONS_NOT_SATISFIED, secondTapResponse)
    }

    @Test
    fun `SELECT AID desconocido devuelve 6A82 sin filtrar el comprobante armado en RAM`() {
        val signedPayload = NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
            amountClp = 3_200L,
            merchantName = "Cafe Grano Centro",
            secretKeyBytes = testSecretKey32Bytes
        )
        HostApduServiceNfc.armAuthorizedPayload(signedPayload.toApduUtf8Bytes())

        val unknownAidApdu = HostApduServiceNfc.buildSelectAidCommandApdu("F0000000000000000001")
        val response = HostApduServiceNfc.evaluateApduCommand(unknownAidApdu)

        assertArrayEquals(HostApduServiceNfc.SW_APP_NOT_FOUND, response)
    }

    @Test
    fun `verificacion en tiempo constante detecta adulteracion de monto CLP o firma HMAC`() {
        val legitimatePayload = NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
            amountClp = 3_200L,
            merchantName = "Cafe Grano Centro",
            secretKeyBytes = testSecretKey32Bytes
        )

        val tamperedAmountPayload = legitimatePayload.copy(amountClp = 320_000L)
        val flippedLastChar = if (legitimatePayload.hmacSha256Hex.endsWith("0")) "1" else "0"
        val tamperedSignaturePayload = legitimatePayload.copy(
            hmacSha256Hex = legitimatePayload.hmacSha256Hex.dropLast(1) + flippedLastChar
        )

        assertFalse(
            NfcTransactionSyncSecurityGuard.verifyPayloadIntegrity(
                tamperedAmountPayload,
                testSecretKey32Bytes
            )
        )
        assertFalse(
            NfcTransactionSyncSecurityGuard.verifyPayloadIntegrity(
                tamperedSignaturePayload,
                testSecretKey32Bytes
            )
        )
    }

    @Test
    fun `reintento con el mismo Idempotency-Key es bloqueado evitando doble cobro en CLP`() {
        val sharedIdempotencyKey = "550e8400-e29b-41d4-a716-446655440000"

        NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
            amountClp = 24_500L,
            merchantName = "Supermercado Barrio",
            secretKeyBytes = testSecretKey32Bytes,
            idempotencyKey = sharedIdempotencyKey
        )

        assertThrows(IllegalStateException::class.java) {
            NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
                amountClp = 24_500L,
                merchantName = "Supermercado Barrio",
                secretKeyBytes = testSecretKey32Bytes,
                idempotencyKey = sharedIdempotencyKey
            )
        }
    }
}
