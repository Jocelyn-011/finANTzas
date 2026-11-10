package cl.aiep.finantzas.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Servicio nativo NFC HCE (Host Card Emulation) bajo ISO/IEC 14443-4 para finANTzas.
 *
 * Responde al comando C-APDU `SELECT AID` (`F046494E414E545A4153`) entregando únicamente
 * un payload de pago en CLP (`Long` sin decimales) previamente autorizado y consumiéndolo
 * en un solo uso (One-Time Token en memoria volátil).
 */
class HostApduServiceNfc : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val response = evaluateApduCommand(commandApdu)
        Log.i(TAG, "Trama APDU procesada (${commandApdu?.size ?: 0} bytes recibidos)")
        return response
    }

    override fun onDeactivated(reason: Int) {
        clearAuthorizedPayload()
        Log.i(TAG, "Enlace NFC desactivado (reason=$reason). Token pendiente limpiado de RAM.")
    }

    companion object {
        private const val TAG = "HostApduServiceNfc"

        /** AID propietario en hexadecimal: 0xF0 + "FINANTZAS" en ASCII (10 bytes). */
        const val FINANTZAS_AID_HEX: String = "F046494E414E545A4153"

        private const val IDX_CLA = 0
        private const val IDX_INS = 1
        private const val IDX_P1 = 2
        private const val IDX_P2 = 3
        private const val IDX_LC = 4
        private const val APDU_HEADER_SIZE = 5
        private const val HEX_CHARS_PER_BYTE = 2
        private const val HEX_RADIX = 16
        private const val UNSIGNED_BYTE_MASK = 0xFF

        private const val CLA_ISO7816: Byte = 0x00
        private const val INS_SELECT: Byte = 0xA4.toByte()
        private const val P1_SELECT_BY_DF_NAME: Byte = 0x04
        private const val P2_FIRST_OR_ONLY: Byte = 0x00

        /** Status Word 90 00: Comando APDU ejecutado correctamente. */
        val SW_OK: ByteArray = byteArrayOf(0x90.toByte(), 0x00)

        /** Status Word 69 85: Pago no armado / falta autenticación biométrica previa. */
        val SW_CONDITIONS_NOT_SATISFIED: ByteArray = byteArrayOf(0x69, 0x85.toByte())

        /** Status Word 6A 82: AID o instrucción APDU no reconocida. */
        val SW_APP_NOT_FOUND: ByteArray = byteArrayOf(0x6A, 0x82.toByte())

        private val FINANTZAS_AID_BYTES: ByteArray = hexToBytes(FINANTZAS_AID_HEX)

        private val authorizedPayloadRef = AtomicReference<ByteArray?>(null)

        /**
         * Arma en RAM el comprobante firmado de un pago en CLP (`Long` sin decimales)
         * tras validar biometría. Solo podrá ser leído una única vez por el POS.
         */
        fun armAuthorizedPayload(payloadUtf8Bytes: ByteArray) {
            authorizedPayloadRef.set(payloadUtf8Bytes.copyOf())
        }

        /** Destruye cualquier comprobante pendiente en memoria volátil. */
        fun clearAuthorizedPayload() {
            authorizedPayloadRef.set(null)
        }

        /**
         * Evalúa la trama C-APDU entrante y construye la respuesta R-APDU ISO/IEC 7816-4.
         * Separada de dependencias de Android para facilitar pruebas unitarias JUnit en el Paso 4.
         */
        internal fun evaluateApduCommand(commandApdu: ByteArray?): ByteArray {
            return if (commandApdu == null || !isSelectFinantzasAid(commandApdu)) {
                SW_APP_NOT_FOUND.copyOf()
            } else {
                val oneTimePayload = authorizedPayloadRef.getAndSet(null)
                if (oneTimePayload != null) {
                    oneTimePayload + SW_OK
                } else {
                    SW_CONDITIONS_NOT_SATISFIED.copyOf()
                }
            }
        }

        /** Construye una trama C-APDU `SELECT AID` válida (útil para simulación y tests JUnit). */
        internal fun buildSelectAidCommandApdu(aidHex: String = FINANTZAS_AID_HEX): ByteArray {
            val aidBytes = hexToBytes(aidHex)
            val header = byteArrayOf(
                CLA_ISO7816,
                INS_SELECT,
                P1_SELECT_BY_DF_NAME,
                P2_FIRST_OR_ONLY,
                aidBytes.size.toByte()
            )
            return header + aidBytes
        }

        private fun isSelectFinantzasAid(apdu: ByteArray): Boolean {
            val aidLength = FINANTZAS_AID_BYTES.size
            val hasMinimumLength = apdu.size >= APDU_HEADER_SIZE + aidLength
            val hasSelectHeader = hasMinimumLength &&
                    apdu[IDX_CLA] == CLA_ISO7816 &&
                    apdu[IDX_INS] == INS_SELECT &&
                    apdu[IDX_P1] == P1_SELECT_BY_DF_NAME &&
                    apdu[IDX_P2] == P2_FIRST_OR_ONLY &&
                    (apdu[IDX_LC].toInt() and UNSIGNED_BYTE_MASK) == aidLength

            return hasSelectHeader &&
                    apdu.copyOfRange(APDU_HEADER_SIZE, APDU_HEADER_SIZE + aidLength)
                        .contentEquals(FINANTZAS_AID_BYTES)
        }

        private fun hexToBytes(hex: String): ByteArray {
            require(hex.length % HEX_CHARS_PER_BYTE == 0) {
                "La cadena hexadecimal debe tener longitud par."
            }
            return ByteArray(hex.length / HEX_CHARS_PER_BYTE) { index ->
                val start = index * HEX_CHARS_PER_BYTE
                hex.substring(start, start + HEX_CHARS_PER_BYTE)
                    .toInt(HEX_RADIX)
                    .toByte()
            }
        }
    }
}
