package cl.finantzas.app.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

class FinantzasAuthRepository {

    private val auth = SupabaseProvider.client.auth

    suspend fun register(email: String, password: String) {
        require(email.isNotBlank()) { "Ingresa tu correo" }
        require(password.length >= 8) {
            "La contraseña debe tener al menos 8 caracteres"
        }

        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun login(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    fun currentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }
}