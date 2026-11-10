package cl.aiep.finantzas.nfc

import cl.aiep.finantzas.domain.rules.PaymentInstrument
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Comprobante inmutable de un pago sin contacto en Pesos Chilenos (`CLP` entero `Long` sin decimales),
 * protegido con clave de idempotencia única y firma criptográfica `HmacSHA256`.
 */
data class ContactlessPaymentClpPayload(
    val amountClp: Long,
    val currency: String,
    val merchantName: String,
    val paymentInstrumentCode: String,
    val idempotencyKey: String,
    val timestampEpochMs: Long,
    val canonicalMessage: String,
    val hmacSha256Hex: String
) {
    /** Serializa el comprobante firmado en UTF-8 listo para transmitir por APDU en [HostApduServiceNfc]. */
    fun toApduUtf8Bytes(): ByteArray {
        return "$canonicalMessage|$hmacSha256Hex".toByteArray(StandardCharsets.UTF_8)
    }
}

/**
 * Guardián de integridad criptográfica (Tríada CID), idempotencia y verificación en tiempo
 * constante (**OWASP M10**) para pagos NFC en Pesos Chilenos (`CLP` sin decimales).
 */
object NfcTransactionSyncSecurityGuard {

    const val CURRENCY_CLP: String = "CLP"
    private const val PROTOCOL_VERSION = "v1"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val MIN_SECRET_KEY_BYTES = 16
    private const val MIN_EXCLUSIVE_AMOUNT_CLP = 0L
    private const val UNSIGNED_BYTE_MASK = 0xFF
    private const val HEX_CHARS_PER_BYTE = 2
    private const val HEX_PAD_CHAR = '0'
    private const val HEX_RADIX = 16
    private const val NIBBLE_SHIFT_BITS = 4

    private val processedIdempotencyKeys: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Construye, registra por idempotencia, firma con `HmacSHA256` y autoverifica en tiempo
     * constante un pago NFC en CLP (`Long` sin decimales, ej. `3200L`, `12990L`, `24500L`).
     */
    fun buildAndVerifyContactlessPaymentClp(
        amountClp: Long,
        merchantName: String,
        secretKeyBytes: ByteArray,
        idempotencyKey: String = UUID.randomUUID().toString(),
        timestampEpochMs: Long = System.currentTimeMillis()
    ): ContactlessPaymentClpPayload {
        val sanitizedMerchant = merchantName.replace("|", "").trim()
        validatePaymentInputs(amountClp, sanitizedMerchant, secretKeyBytes, idempotencyKey)

        val registeredAsNew = processedIdempotencyKeys.add(idempotencyKey)
        check(registeredAsNew) {
            "Transacción duplicada bloqueada: el Idempotency-Key '$idempotencyKey' ya fue procesado."
        }

        val canonicalMessage = buildCanonicalMessageClp(
            amountClp = amountClp,
            merchantName = sanitizedMerchant,
            idempotencyKey = idempotencyKey,
            timestampEpochMs = timestampEpochMs
        )
        val signatureHex = bytesToHex(computeHmacSha256Bytes(canonicalMessage, secretKeyBytes))

        val signedPayload = ContactlessPaymentClpPayload(
            amountClp = amountClp,
            currency = CURRENCY_CLP,
            merchantName = sanitizedMerchant,
            paymentInstrumentCode = PaymentInstrument.CODE_NFC_WALLET,
            idempotencyKey = idempotencyKey,
            timestampEpochMs = timestampEpochMs,
            canonicalMessage = canonicalMessage,
            hmacSha256Hex = signatureHex
        )

        check(verifyPayloadIntegrity(signedPayload, secretKeyBytes)) {
            "Fallo crítico de integridad CID al verificar la firma HmacSHA256 del pago CLP."
        }

        return signedPayload
    }

    /**
     * Verifica la integridad `HmacSHA256` de un comprobante en CLP en tiempo constante
     * mediante [MessageDigest.isEqual], mitigando ataques de temporización (**OWASP M10**).
     */
    fun verifyPayloadIntegrity(
        payload: ContactlessPaymentClpPayload,
        secretKeyBytes: ByteArray
    ): Boolean {
        val sanitizedMerchant = payload.merchantName.replace("|", "").trim()
        val expectedCanonical = buildCanonicalMessageClp(
            amountClp = payload.amountClp,
            merchantName = sanitizedMerchant,
            idempotencyKey = payload.idempotencyKey,
            timestampEpochMs = payload.timestampEpochMs
        )
        val canonicalMatches = MessageDigest.isEqual(
            expectedCanonical.toByteArray(StandardCharsets.UTF_8),
            payload.canonicalMessage.toByteArray(StandardCharsets.UTF_8)
        )
        val signatureMatches = verifyHmacConstantTime(
            canonicalMessage = expectedCanonical,
            candidateHmacHex = payload.hmacSha256Hex,
            secretKeyBytes = secretKeyBytes
        )
        return canonicalMatches && signatureMatches
    }

    /**
     * Compara en tiempo constante los 32 bytes del MAC `HmacSHA256` esperado frente a la
     * firma hexadecimal recibida usando [MessageDigest.isEqual].
     */
    fun verifyHmacConstantTime(
        canonicalMessage: String,
        candidateHmacHex: String,
        secretKeyBytes: ByteArray
    ): Boolean {
        val candidateBytes = hexToBytesOrNull(candidateHmacHex)
        val expectedBytes = computeHmacSha256Bytes(canonicalMessage, secretKeyBytes)
        return candidateBytes != null && MessageDigest.isEqual(expectedBytes, candidateBytes)
    }

    /** Calcula el código de autenticación de mensaje (MAC) crudo con `HmacSHA256`. */
    internal fun computeHmacSha256Bytes(
        canonicalMessage: String,
        secretKeyBytes: ByteArray
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKeySpec = SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM)
        mac.init(secretKeySpec)
        return mac.doFinal(canonicalMessage.toByteArray(StandardCharsets.UTF_8))
    }

    /** Limpia el registro en memoria de claves de idempotencia (útil para aislamiento en tests). */
    fun clearProcessedIdempotencyKeys() {
        processedIdempotencyKeys.clear()
    }

    private fun validatePaymentInputs(
        amountClp: Long,
        sanitizedMerchant: String,
        secretKeyBytes: ByteArray,
        idempotencyKey: String
    ) {
        require(amountClp > MIN_EXCLUSIVE_AMOUNT_CLP) {
            "El monto en CLP debe ser un entero positivo mayor a cero (sin decimales)."
        }
        require(sanitizedMerchant.isNotEmpty()) {
            "El nombre del comercio no puede estar vacío."
        }
        require(secretKeyBytes.size >= MIN_SECRET_KEY_BYTES) {
            "La clave secreta HMAC debe tener al menos $MIN_SECRET_KEY_BYTES bytes de entropía."
        }
        require(idempotencyKey.isNotBlank()) {
            "El Idempotency-Key no puede estar vacío."
        }
    }

    private fun buildCanonicalMessageClp(
        amountClp: Long,
        merchantName: String,
        idempotencyKey: String,
        timestampEpochMs: Long
    ): String {
        val instrument = PaymentInstrument.CODE_NFC_WALLET
        return "$PROTOCOL_VERSION|$CURRENCY_CLP|$amountClp|$merchantName|$instrument|$idempotencyKey|$timestampEpochMs"
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * HEX_CHARS_PER_BYTE)
        for (byte in bytes) {
            val unsigned = byte.toInt() and UNSIGNED_BYTE_MASK
            builder.append(unsigned.toString(HEX_RADIX).padStart(HEX_CHARS_PER_BYTE, HEX_PAD_CHAR))
        }
        return builder.toString()
    }

    private fun hexToBytesOrNull(hex: String): ByteArray? {
        val isValidHex = hex.length % HEX_CHARS_PER_BYTE == 0 &&
                hex.all { char -> char.digitToIntOrNull(HEX_RADIX) != null }
        return if (!isValidHex) {
            null
        } else {
            ByteArray(hex.length / HEX_CHARS_PER_BYTE) { index ->
                val start = index * HEX_CHARS_PER_BYTE
                val highNibble = hex[start].digitToInt(HEX_RADIX) shl NIBBLE_SHIFT_BITS
                val lowNibble = hex[start + 1].digitToInt(HEX_RADIX)
                (highNibble or lowNibble).toByte()
            }
        }
    }
}
