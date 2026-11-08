package cl.finantzas.app.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.finantzas.app.data.local.security.AndroidKeystoreVault
import java.security.KeyStore
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreSecurityTest {
    @Test
    fun passphraseIsStableAndMasterKeyIsNotExportable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = AndroidKeystoreVault(context).getOrCreatePassphrase()
        val second = AndroidKeystoreVault(context).getOrCreatePassphrase()
        try {
            assertEquals(32, first.size)
            assertTrue("Passphrase must survive vault recreation", first.contentEquals(second))
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = store.getKey("finantzas_sqlcipher_master_key", null)
            assertNotNull(key)
            assertNull(key.encoded)
        } finally {
            first.fill(0)
            second.fill(0)
        }
    }
}
