package cl.finantzas.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.remote.FinantzasAuthRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit
) {
    val repository = remember { FinantzasAuthRepository() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "finANTzas",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (registering) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                loading = true
                message = ""

                scope.launch {
                    try {
                        if (registering) {
                            repository.register(email, password)

                            if (repository.currentUserId() != null) {
                                onAuthenticated()
                            } else {
                                message = "Cuenta creada. Revisa tu correo para confirmar el registro."
                            }
                        } else {
                            repository.login(email, password)
                            if (repository.currentUserId() != null) {
                                onAuthenticated()
                            } else {
                                message = "No se pudo iniciar sesión. Verifica tu correo."
                            }
                        }
                    } catch (e: Exception) {
                        message = e.message ?: "Ocurrió un error de autenticación."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (loading) "Procesando..."
                else if (registering) "Crear cuenta"
                else "Ingresar"
            )
        }

        TextButton(
            enabled = !loading,
            onClick = {
                registering = !registering
                message = ""
            }
        ) {
            Text(
                if (registering) "Ya tengo cuenta"
                else "Crear una cuenta nueva"
            )
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}