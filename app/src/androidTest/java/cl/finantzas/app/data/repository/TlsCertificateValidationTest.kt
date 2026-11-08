package cl.finantzas.app.data.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import java.io.Closeable
import java.math.BigInteger
import java.net.InetAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509KeyManager
import javax.security.auth.x500.X500Principal
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TlsCertificateValidationTest {
    @Test
    fun defaultOkHttpEngineRejectsUntrustedLoopbackCertificate() {
        runBlocking {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "finantzas_tls_audit_${UUID.randomUUID()}"
            check(!store.containsAlias(alias))
            try {
                val identity = createTestIdentity(store, alias)
                LoopbackTlsServer(identity).use { server ->
                    val client = HttpClient(OkHttp) {
                        followRedirects = false
                        install(HttpTimeout) {
                            requestTimeoutMillis = TIMEOUT_MS
                            connectTimeoutMillis = TIMEOUT_MS
                            socketTimeoutMillis = TIMEOUT_MS
                        }
                    }
                    try {
                        assertEquals("io.ktor.client.engine.okhttp.OkHttpEngine", client.engine.javaClass.name)
                        var rejection: Throwable? = null
                        try {
                            withTimeout(TIMEOUT_MS * 2) { client.get(server.url) }
                        } catch (failure: Exception) {
                            rejection = failure
                        }
                        val causes = generateSequence(rejection) { it.cause }.take(16).toList()
                        assertTrue("TLS must reject the certificate during handshake, not by timeout or DNS",
                            causes.any { it is SSLHandshakeException })
                        assertTrue("The certificate chain must fail PKIX trust validation",
                            causes.any { it is CertPathValidatorException })
                        assertTrue("The local TLS peer must observe the aborted handshake", server.peerRejected())
                        println("TLS_AUDIT_RESULT=REJECTED_UNTRUSTED_CERTIFICATE")
                        println("TLS_AUDIT_ENGINE=Ktor_OkHttp_DEFAULT_TLS; ENDPOINT=LOOPBACK_ONLY")
                    } finally {
                        client.close()
                    }
                }
            } finally {
                // Delete only the unique identity owned by this test; never the SQLCipher key.
                if (store.containsAlias(alias)) store.deleteEntry(alias)
                assertFalse("The ephemeral test identity must be removed", store.containsAlias(alias))
            }
            println("TLS_AUDIT_CLEANUP=EPHEMERAL_IDENTITY_REMOVED; ROOT_CA_INSTALLED=false")
        }
    }

    private fun createTestIdentity(store: KeyStore, alias: String): X509KeyManager {
        val now = System.currentTimeMillis()
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        generator.initialize(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                // Conscrypt may request raw ECDSA signatures for the isolated TLS server.
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                .setCertificateSubject(X500Principal("CN=finantzas-tls-test.invalid"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(Date(now - 60_000))
                .setCertificateNotAfter(Date(now + 3_600_000))
                .build()
        )
        val pair = generator.generateKeyPair()
        val certificate = store.getCertificate(alias) as X509Certificate
        certificate.checkValidity()
        certificate.verify(pair.public)
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal)
        // This key manager supplies a server identity only; it does not alter client trust.
        return object : X509KeyManager {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
            override fun chooseClientAlias(
                keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?
            ): String? = null
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? =
                if (keyType == "EC") arrayOf(alias) else null
            override fun chooseServerAlias(
                keyType: String?, issuers: Array<out Principal>?, socket: Socket?
            ): String? = getServerAliases(keyType, issuers)?.firstOrNull()
            override fun getCertificateChain(requestedAlias: String?): Array<X509Certificate>? =
                if (requestedAlias == alias) arrayOf(certificate) else null
            override fun getPrivateKey(requestedAlias: String?): PrivateKey? =
                if (requestedAlias == alias) pair.private else null
        }
    }

    private class LoopbackTlsServer(identity: X509KeyManager) : Closeable {
        private val context = SSLContext.getInstance("TLS").apply {
            init(arrayOf(identity), null, SecureRandom())
        }
        private val socket = context.serverSocketFactory.createServerSocket(
            0, 1, InetAddress.getByName("127.0.0.1")
        ) as SSLServerSocket
        private val executor = Executors.newSingleThreadExecutor()
        private val result = executor.submit<Boolean> {
            (socket.accept() as SSLSocket).use { peer ->
                peer.soTimeout = TIMEOUT_MS.toInt()
                try {
                    peer.startHandshake()
                    false
                } catch (_: SSLException) {
                    true
                }
            }
        }
        val url: String get() = "https://127.0.0.1:${socket.localPort}/tls-audit"

        init { socket.soTimeout = TIMEOUT_MS.toInt() }

        fun peerRejected(): Boolean = result.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)

        override fun close() {
            socket.close()
            executor.shutdownNow()
            check(executor.awaitTermination(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                "The isolated TLS server must terminate"
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
