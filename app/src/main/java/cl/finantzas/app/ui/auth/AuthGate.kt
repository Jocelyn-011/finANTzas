package cl.finantzas.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cl.finantzas.app.data.remote.FinantzasAuthRepository
import cl.finantzas.app.data.remote.SupabaseProvider
import cl.finantzas.app.ui.navigation.FinANTzasNavigation
import cl.finantzas.app.workers.ExpenseSyncScheduler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun AuthGate() {
    val client = remember { SupabaseProvider.client }
    val context = LocalContext.current

    var authenticated by remember {
        mutableStateOf<Boolean?>(null)
    }


    LaunchedEffect(client) {
        client.auth.awaitInitialization()
        client.auth.sessionStatus.collect {
            authenticated = client.auth.currentUserOrNull() != null
        }
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

        true -> AuthenticatedContent { authenticated = false }
    }
}

@Composable
private fun AuthenticatedContent(onSignedOut: () -> Unit) {
    val repository = remember { FinantzasAuthRepository() }
    val scope = rememberCoroutineScope()
    var loggingOut by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(
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
                                onSignedOut()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                error = "No se pudo cerrar la sesion. Intenta nuevamente."
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
