package cl.finantzas.app.data.nfc

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val DEMO_KEY_ALIAS = "finantzas.nfc.simulation.signing.v1"
internal const val NFC_DEMO_SIGNATURE_ALGORITHM = "SHA256withECDSA"
private val envelopeJson = Json { encodeDefaults = true }

/** Firma para la demostración local; ningún servidor registra ni valida todavía esta clave. */
class NfcDemoSigner {
    fun sign(payload: NfcDemoPayloadClp): SignedNfcWebhookEnvelopeClp =
        signNfcDemoPayload(envelopeJson.encodeToString(payload), getOrCreateDemoKey())

    @Synchronized
    private fun getOrCreateDemoKey(): KeyPair {
        val store = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = store.getKey(DEMO_KEY_ALIAS, null) as? PrivateKey
        val certificate = store.getCertificate(DEMO_KEY_ALIAS)
        if (existing != null && certificate != null) {
            return KeyPair(certificate.publicKey, existing)
        }

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
        generator.initialize(
            KeyGenParameterSpec.Builder(DEMO_KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return generator.generateKeyPair()
    }
}

internal fun signNfcDemoPayload(payloadJson: String, keyPair: KeyPair): SignedNfcWebhookEnvelopeClp {
    val signer = Signature.getInstance(NFC_DEMO_SIGNATURE_ALGORITHM).apply {
        initSign(keyPair.private)
        update(payloadJson.toByteArray(Charsets.UTF_8))
    }
    return SignedNfcWebhookEnvelopeClp(
        payloadJson = payloadJson,
        algorithm = NFC_DEMO_SIGNATURE_ALGORITHM,
        signatureBase64 = Base64.getEncoder().encodeToString(signer.sign()),
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
    )
}
