package cl.finantzas.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cl.finantzas.app.R
import cl.finantzas.app.data.remote.FinantzasAuthRepository
import cl.finantzas.app.data.remote.SupabaseProvider
import cl.finantzas.app.ui.navigation.FinANTzasNavigation
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBg
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.workers.ExpenseSyncScheduler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthGate() {
    val client = remember { SupabaseProvider.client }
    val context = LocalContext.current

    var authenticated by remember {
        mutableStateOf<Boolean?>(null)
    }
    var showOpeningSplash by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        delay(1_200L)
        showOpeningSplash = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        when (authenticated) {
            null -> FinantzasOpeningSplashScreen()

            false -> AuthScreen(
                onAuthenticated = {
                    authenticated = true
                }
            )

            true -> AuthenticatedContent { authenticated = false }
        }

        AnimatedVisibility(
            visible = showOpeningSplash,
            exit = fadeOut()
        ) {
            FinantzasOpeningSplashScreen()
        }
    }
}

@Composable
private fun FinantzasOpeningSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.finantzas_splash_logo),
                contentDescription = "Logo FinANTzas",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(280.dp)
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = MintBright,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AuthenticatedContent(onSignedOut: () -> Unit) {
    val repository = remember { FinantzasAuthRepository() }
    val scope = rememberCoroutineScope()
    var loggingOut by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPetrolBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
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
                    text = if (loggingOut) "Saliendo..." else "Cerrar sesion",
                    color = MintBright
                )
            }
        }

        if (error.isNotBlank()) {
            Text(
                text = error,
                color = CoralHormiga,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            FinANTzasNavigation()
        }
    }
}
