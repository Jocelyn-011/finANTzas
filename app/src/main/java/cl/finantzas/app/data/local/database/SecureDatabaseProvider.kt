package cl.finantzas.app.data.local.database

import android.content.Context
import androidx.room.Room
import cl.finantzas.app.data.local.security.AndroidKeystoreVault
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object SecureDatabaseProvider {

    @Volatile
    private var INSTANCE: FinantzasDatabase? = null

    fun getDatabase(context: Context): FinantzasDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: createDatabase(context.applicationContext)
                .also { INSTANCE = it }
        }
    }

    private fun createDatabase(context: Context): FinantzasDatabase {

        // Cargar la biblioteca nativa de SQLCipher
        System.loadLibrary("sqlcipher")

        // Recuperar la contraseña protegida por Android Keystore
        val vault = AndroidKeystoreVault(context)
        val passphrase = vault.getOrCreatePassphrase()

        // Configurar SQLCipher
        val factory = SupportOpenHelperFactory(passphrase)

        // Crear la base de datos Room con cifrado SQLCipher
        return Room.databaseBuilder(
            context,
            FinantzasDatabase::class.java,
            "finantzas_secure.db"
        )
            .openHelperFactory(factory)
            .build()
    }
}