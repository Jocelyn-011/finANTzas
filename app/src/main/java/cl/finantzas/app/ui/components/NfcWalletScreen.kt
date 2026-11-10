package cl.finantzas.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.ClpImpactCalculator
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.nfc.NfcDemoMerchant
import cl.finantzas.app.data.nfc.NfcDemoSigner
import cl.finantzas.app.data.nfc.SignedNfcWebhookEnvelopeClp
import cl.finantzas.app.data.nfc.createNfcDemoExpense
import cl.finantzas.app.data.nfc.toNfcDemoPayload
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.workers.ExpenseSyncScheduler
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Billetera de demostración: no procesa pagos ni usa una tarjeta real. */
@Composable
fun NfcWalletScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = remember(context) { context.findActivity() }
    val userId = activeExpenseUser()
    val repository = remember(appContext) {
        RoomExpenseRepository(SecureDatabaseProvider.getDatabase(appContext).expenseDao())
    }
    val signer = remember { NfcDemoSigner() }
    val scope = rememberCoroutineScope()
    var selectedMerchant by remember { mutableStateOf<NfcDemoMerchant?>(null) }
    var selectedFrequency by remember { mutableStateOf<Int?>(null) }
    var processing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var latestEnvelope by remember { mutableStateOf<SignedNfcWebhookEnvelopeClp?>(null) }

    DisposableEffect(activity) {
        val window = activity?.window
        val wasSecure = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Billetera virtual NFC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Simulación: no se realizan pagos reales.", style = MaterialTheme.typography.bodyMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tarjeta de demostración", style = MaterialTheme.typography.titleMedium)
                Text("•••• •••• •••• 4829", style = MaterialTheme.typography.headlineSmall)
                Text("Número ficticio para la simulación", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("Comercios de prueba", style = MaterialTheme.typography.titleMedium)
        Text("Selecciona un comercio para preparar una simulación.", style = MaterialTheme.typography.bodySmall)
        NfcDemoMerchant.entries.forEach { merchant ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !processing,
                onClick = { selectedFrequency = null; selectedMerchant = merchant }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(merchant.displayName, modifier = Modifier.weight(1f))
                    Text(ClpCurrencyFormatter.formatClp(merchant.amountClp))
                }
            }
        }

        Text(
            "La simulación se registra como gasto periódico, según la frecuencia que selecciones. No hay cobro real.",
            style = MaterialTheme.typography.bodySmall
        )

        if (processing) Text("Preparando simulación…")
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        latestEnvelope?.let { envelope ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sobre firmado de demostración", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Firma generada en este dispositivo. Todavía no hay un servicio conectado que reciba o valide este sobre.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Datos: " + envelope.payloadJson, style = MaterialTheme.typography.bodySmall)
                            Text("Algoritmo: " + envelope.algorithm, style = MaterialTheme.typography.bodySmall)
                            Text("Firma: " + envelope.signatureBase64, style = MaterialTheme.typography.bodySmall)
                            Text("Clave pública: " + envelope.publicKeyBase64, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    selectedMerchant?.let { merchant ->
        AlertDialog(
            onDismissRequest = { if (!processing) selectedMerchant = null },
            title = { Text("Confirmar simulación") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(merchant.displayName + " · " + ClpCurrencyFormatter.formatClp(merchant.amountClp))
                    Text("Elige la frecuencia del gasto simulado:", style = MaterialTheme.typography.bodyMedium)
                    listOf(0 to "Mensual fijo", 1 to "1 vez por semana", 4 to "4 veces por semana", 7 to "7 veces por semana")
                        .forEach { (frequency, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedFrequency == frequency, onClick = { selectedFrequency = frequency })
                                Text(label)
                            }
                        }
                    selectedFrequency?.let { frequency ->
                        val impact = ClpImpactCalculator.monthlyImpactClp(merchant.amountClp, frequency)
                        val preview = createNfcDemoExpense(merchant, frequency, userId, "preview", 0L)
                        Text("Impacto mensual estimado: ${ClpCurrencyFormatter.formatClp(impact)}")
                        Text(if (preview.isAntExpense) "Clasificación: gasto hormiga (${preview.triggeredRuleCode})" else "Clasificación: gasto esencial")
                    }
                    Text("Se guardará en tus gastos y afectará tus gráficos y proyecciones. No se realizará ningún cobro.")
                }
            },
            confirmButton = {
                Button(
                    enabled = !processing && selectedFrequency != null,
                    onClick = {
                        val frequency = selectedFrequency
                        if (!processing && frequency != null) {
                            processing = true
                            selectedMerchant = null
                            message = null
                            scope.launch {
                                try {
                                    val expense = createNfcDemoExpense(
                                        merchant = merchant,
                                        weeklyFrequency = frequency,
                                        userId = userId,
                                        expenseId = UUID.randomUUID().toString(),
                                        createdAt = System.currentTimeMillis()
                                    )
                                    val envelope = withContext(Dispatchers.IO) {
                                        signer.sign(expense.toNfcDemoPayload(merchant))
                                    }
                                    val syncScheduled = withContext(NonCancellable) {
                                        repository.insert(expense)
                                        if (userId == LOCAL_EXPENSE_USER) false else try {
                                            ExpenseSyncScheduler.schedule(appContext)
                                            true
                                        } catch (_: Exception) {
                                            false
                                        }
                                    }
                                    latestEnvelope = envelope
                                    message = when {
                                        userId == LOCAL_EXPENSE_USER -> "Simulación guardada en este dispositivo."
                                        syncScheduled -> "Simulación guardada. Se intentará sincronizar con tu cuenta."
                                        else -> "Simulación guardada, pero no se pudo programar la sincronización."
                                    }
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    message = "No se pudo preparar o guardar la simulación. Inténtalo nuevamente."
                                } finally {
                                    processing = false
                                }
                            }
                        }
                    }
                ) { Text("Registrar simulación") }
            },
            dismissButton = { TextButton(onClick = { selectedMerchant = null }) { Text("Cancelar") } },
            properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
