package cl.finantzas.app.data.local.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreVault(context: Context) {

    private val alias = "finantzas_sqlcipher_master_key"

    private val preferences = context.applicationContext
        .getSharedPreferences("finantzas_secure_vault", Context.MODE_PRIVATE)

    private val passphraseKey = "encrypted_sqlcipher_passphrase"
    private val ivKey = "sqlcipher_passphrase_iv"

    // Crear o recuperar la clave AES-256 del Android Keystore
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val existingKey = keyStore.getKey(alias, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    // Generar o recuperar la contraseña protegida de SQLCipher
    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        val encrypted = preferences.getString(passphraseKey, null)
        val storedIv = preferences.getString(ivKey, null)

        if (encrypted != null && storedIv != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    128,
                    Base64.decode(storedIv, Base64.NO_WRAP)
                )
            )

            return cipher.doFinal(
                Base64.decode(encrypted, Base64.NO_WRAP)
            )
        }

        // No regenerar si existe información incompleta
        check(encrypted == null && storedIv == null) {
            "Los datos del almacén seguro están incompletos"
        }

        val passphrase = ByteArray(32)
        SecureRandom().nextBytes(passphrase)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val encryptedPassphrase = cipher.doFinal(passphrase)

        val saved = preferences.edit()
            .putString(
                passphraseKey,
                Base64.encodeToString(
                    encryptedPassphrase,
                    Base64.NO_WRAP
                )
            )
            .putString(
                ivKey,
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP
                )
            )
            .commit()

        check(saved) {
            "No se pudo guardar la contraseña cifrada"
        }

        return passphrase
    }
}