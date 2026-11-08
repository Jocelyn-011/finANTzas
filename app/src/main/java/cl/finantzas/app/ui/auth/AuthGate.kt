package cl.finantzas.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cl.finantzas.app.data.remote.FinantzasAuthRepository
import cl.finantzas.app.data.remote.SupabaseProvider
import cl.finantzas.app.ui.navigation.FinANTzasNavigation
import cl.finantzas.app.workers.ExpenseSyncScheduler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun AuthGate() {
    val repository = remember { FinantzasAuthRepository() }
    val client = remember { SupabaseProvider.client }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var authenticated by remember {
        mutableStateOf<Boolean?>(null)
    }

    var loggingOut by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        authenticated = client.auth.currentUserOrNull() != null
    }

    LaunchedEffect(authenticated) {
        if (authenticated == true) {
            ExpenseSyncScheduler.schedule(context)
        }
    }

    when (authenticated) {
        null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        false -> AuthScreen(
            onAuthenticated = {
                authenticated = true
            }
        )

        true -> Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    enabled = !loggingOut,
                    onClick = {
                        loggingOut = true
                        error = ""

                        scope.launch {
                            try {
                                repository.logout()
                                authenticated = false
                            } catch (e: Exception) {
                                error = "Error: ${e.message ?: e.javaClass.simpleName}"
                            } finally {
                                loggingOut = false
                            }
                        }
                    }
                ) {
                    Text(
                        if (loggingOut) "Saliendo..."
                        else "Cerrar sesion"
                    )
                }
            }

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                FinANTzasNavigation()
            }
        }
    }
}