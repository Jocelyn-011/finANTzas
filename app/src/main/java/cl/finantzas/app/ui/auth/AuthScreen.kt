package cl.finantzas.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.remote.FinantzasAuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private class AuthFormState {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var registering by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var message by mutableStateOf("")

    suspend fun authenticate(repository: FinantzasAuthRepository, onAuthenticated: () -> Unit) {
        loading = true
        message = ""
        try {
            if (registering) repository.register(email, password) else repository.login(email, password)
            if (repository.currentUserId() != null) {
                password = ""
                onAuthenticated()
            } else {
                message = if (registering) "Cuenta creada. Revisa tu correo para confirmar el registro."
                    else "No se pudo iniciar sesion. Verifica tu correo."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            message = "No se pudo autenticar. Revisa tus datos y la conexion."
        } finally {
            loading = false
        }
    }
}

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val repository = remember { FinantzasAuthRepository() }
    val scope = rememberCoroutineScope()
    val state = remember { AuthFormState() }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("finANTzas", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.registering) "Crear cuenta" else "Iniciar sesi\u00f3n",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(24.dp))
        AuthFields(state)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { scope.launch { state.authenticate(repository, onAuthenticated) } },
            enabled = !state.loading && state.email.isNotBlank() && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.loading) "Procesando..." else if (state.registering) "Crear cuenta" else "Ingresar")
        }
        TextButton(enabled = !state.loading, onClick = {
            state.registering = !state.registering
            state.message = ""
        }) {
            Text(if (state.registering) "Ya tengo cuenta" else "Crear una cuenta nueva")
        }
        if (state.message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AuthFields(state: AuthFormState) {
    OutlinedTextField(
        value = state.email,
        onValueChange = { state.email = it },
        label = { Text("Correo electr\u00f3nico") },
        singleLine = true,
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = { state.password = it },
        label = { Text("Contrase\u00f1a") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth()
    )
}
