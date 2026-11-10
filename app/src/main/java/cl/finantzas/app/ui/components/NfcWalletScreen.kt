package cl.finantzas.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.nfc.BiometricAuthOutcome
import cl.aiep.finantzas.nfc.ContactlessPaymentClpPayload
import cl.aiep.finantzas.nfc.HostApduServiceNfc
import cl.aiep.finantzas.nfc.NfcBiometricAuthorizer
import cl.aiep.finantzas.nfc.NfcTransactionSyncSecurityGuard
import cl.aiep.finantzas.nfc.findFragmentActivity
import cl.finantzas.app.data.local.security.AndroidKeystoreVault
import cl.finantzas.app.ui.theme.Emerald
import cl.finantzas.app.ui.theme.Petrol

private data class PresetNfcPurchaseClp(
    val merchantName: String,
    val amountClp: Long
)

private const val CAFE_AMOUNT_CLP = 3_200L
private const val LUNCH_AMOUNT_CLP = 12_990L
private const val GROCERY_AMOUNT_CLP = 24_500L
private const val HMAC_PREVIEW_LENGTH = 16

private val presetPurchasesClp = listOf(
    PresetNfcPurchaseClp("Café Grano Centro", CAFE_AMOUNT_CLP),
    PresetNfcPurchaseClp("Almuerzo Ejecutivo", LUNCH_AMOUNT_CLP),
    PresetNfcPurchaseClp("Supermercado Barrio", GROCERY_AMOUNT_CLP)
)

@Composable
fun NfcWalletScreen() {
    val context = LocalContext.current
    var selectedPurchase by remember { mutableStateOf(presetPurchasesClp.first()) }
    var armedPayload by remember { mutableStateOf<ContactlessPaymentClpPayload?>(null) }
    var statusMessage by remember {
        mutableStateOf("Selecciona un monto en CLP y autentica con biometría antes de acercar al POS.")
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Billetera Sin Contacto NFC (CLP)",
            style = MaterialTheme.typography.titleLarge,
            color = Petrol
        )
        PurchasePresetSelector(
            selected = selectedPurchase,
            onSelect = { purchase ->
                HostApduServiceNfc.clearAuthorizedPayload()
                armedPayload = null
                selectedPurchase = purchase
                statusMessage = "Monto seleccionado: ${ClpCurrencyFormatter.formatClp(purchase.amountClp)} CLP."
            }
        )
        AuthorizeNfcPaymentButton(
            amountClp = selectedPurchase.amountClp,
            onClick = {
                authorizeAndSignClpPayment(
                    context = context,
                    purchase = selectedPurchase,
                    onSigned = { payload ->
                        armedPayload = payload
                        statusMessage = "Pago armado en HCE. Acerca tu teléfono al lector POS NFC."
                    },
                    onFailure = { reason ->
                        armedPayload = null
                        statusMessage = reason
                    }
                )
            }
        )
        Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium)
        armedPayload?.let { payload ->
            ArmedReceiptCard(
                payload = payload,
                onCancelToken = {
                    HostApduServiceNfc.clearAuthorizedPayload()
                    armedPayload = null
                    statusMessage = "Token NFC anulado manualmente de la memoria RAM."
                }
            )
        }
    }
}

@Composable
private fun AuthorizeNfcPaymentButton(
    amountClp: Long,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        val formatted = ClpCurrencyFormatter.formatClp(amountClp)
        Text("Autorizar con Biometría ($formatted CLP)")
    }
}

@Composable
private fun PurchasePresetSelector(
    selected: PresetNfcPurchaseClp,
    onSelect: (PresetNfcPurchaseClp) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Compras rápidas de prueba (Pesos Chilenos sin decimales):",
            style = MaterialTheme.typography.labelLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presetPurchasesClp.forEach { item ->
                FilterChip(
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    label = { Text(ClpCurrencyFormatter.formatClp(item.amountClp)) }
                )
            }
        }
        Text(
            text = "Comercio: ${selected.merchantName} | AID: ${HostApduServiceNfc.FINANTZAS_AID_HEX}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ArmedReceiptCard(
    payload: ContactlessPaymentClpPayload,
    onCancelToken: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Token HCE Armado (Uso Único)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Emerald
            )
            Text("Monto: ${ClpCurrencyFormatter.formatClp(payload.amountClp)} ${payload.currency}")
            Text("Comercio: ${payload.merchantName}")
            Text("Idempotency-Key: ${payload.idempotencyKey}")
            Text("HMAC-SHA256: ${payload.hmacSha256Hex.take(HMAC_PREVIEW_LENGTH)}...")
            OutlinedButton(onClick = onCancelToken, modifier = Modifier.fillMaxWidth()) {
                Text("Anular token NFC en RAM")
            }
        }
    }
}

private fun authorizeAndSignClpPayment(
    context: Context,
    purchase: PresetNfcPurchaseClp,
    onSigned: (ContactlessPaymentClpPayload) -> Unit,
    onFailure: (String) -> Unit
) {
    val activity = context.findFragmentActivity()
    if (activity == null) {
        onFailure("No se encontró una actividad compatible para BiometricPrompt.")
        return
    }
    val vault = AndroidKeystoreVault(context.applicationContext)
    NfcBiometricAuthorizer(activity).authenticateContactlessPaymentClp(
        amountClp = purchase.amountClp,
        merchantName = purchase.merchantName
    ) { outcome ->
        when (outcome) {
            BiometricAuthOutcome.Succeeded -> {
                val secretKey = vault.getOrCreatePassphrase()
                val signedPayload = NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
                    amountClp = purchase.amountClp,
                    merchantName = purchase.merchantName,
                    secretKeyBytes = secretKey
                )
                secretKey.fill(0)
                HostApduServiceNfc.armAuthorizedPayload(signedPayload.toApduUtf8Bytes())
                onSigned(signedPayload)
            }
            BiometricAuthOutcome.AttemptFailed ->
                onFailure("Huella o rostro no reconocido. Intenta nuevamente.")
            is BiometricAuthOutcome.Error ->
                onFailure("Autenticación cancelada o fallida: ${outcome.message}")
        }
    }
}
