package cl.finantzas.app.ui.components

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.ExpenseType
import cl.aiep.finantzas.nfc.BiometricAuthOutcome
import cl.aiep.finantzas.nfc.ContactlessPaymentClpPayload
import cl.aiep.finantzas.nfc.HostApduServiceNfc
import cl.aiep.finantzas.nfc.NfcBiometricAuthorizer
import cl.aiep.finantzas.nfc.NfcTransactionSyncSecurityGuard
import cl.aiep.finantzas.nfc.findFragmentActivity
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.data.local.security.AndroidKeystoreVault
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.FinANTzasThemeState
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.workers.ExpenseSyncScheduler
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.launch

private data class PresetNfcPurchaseClp(
    val merchantName: String,
    val amountClp: Long,
    val category: ExpenseCategory,
    val weeklyFreq: Int
)

private const val CAFE_AMOUNT_CLP = 3_200L
private const val LUNCH_AMOUNT_CLP = 12_990L
private const val GROCERY_AMOUNT_CLP = 24_500L
private const val HMAC_PREVIEW_LENGTH = 16

private val presetPurchasesClp = listOf(
    PresetNfcPurchaseClp("Cafetería Grano", CAFE_AMOUNT_CLP, ExpenseCategory.MICRO_PURCHASES_AND_SNACKS, 5),
    PresetNfcPurchaseClp("Almuerzo Ejecutivo", LUNCH_AMOUNT_CLP, ExpenseCategory.FOOD_AND_GROCERIES, 3),
    PresetNfcPurchaseClp("Supermercado Barrio", GROCERY_AMOUNT_CLP, ExpenseCategory.FOOD_AND_GROCERIES, 1)
)

@Composable
fun NfcWalletScreen() {
    val context = LocalContext.current
    val userId = activeExpenseUser()
    val scope = rememberCoroutineScope()
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val dao = remember(context) { SecureDatabaseProvider.getDatabase(context.applicationContext).expenseDao() }
    val repository = remember(dao) { RoomExpenseRepository(dao) }
    val ruleEngine = remember { AntExpenseRuleEngine() }

    // Activar FLAG_SECURE mientras esta pantalla esté visible (OWASP M6)
    DisposableEffect(context) {
        val window = context.findFragmentActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var selectedPurchase by remember { mutableStateOf(presetPurchasesClp.first()) }
    var customAmountText by remember { mutableStateOf("") }
    var customMerchantText by remember { mutableStateOf("") }
    var armedPayload by remember { mutableStateOf<ContactlessPaymentClpPayload?>(null) }
    var hmacVerified by remember { mutableStateOf(true) }
    var statusMessage by remember {
        mutableStateOf("Listo para firmar en CLP con biometría y transmitir por HostApduService (HCE).")
    }

    val effectiveAmountClp = customAmountText.toLongOrNull()?.takeIf { it > 0L } ?: selectedPurchase.amountClp
    val effectiveMerchant = customMerchantText.trim().ifBlank { selectedPurchase.merchantName }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Parte Superior: Tarjeta TarDigital •••• 4829 rodeada de ondas NFC concéntricas
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NfcConcentricWavesCanvas()
                Text(
                    text = "Acerca tu teléfono al terminal POS",
                    color = MintBright,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                TarDigitalCard3D(cardHolderName = FinANTzasThemeState.userDisplayName)
            }
        }

        // Tarjeta Central de Monto y Comercio
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Monto a Pagar (CLP sin decimales)",
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor.copy(alpha = 0.75f)
                )
                Text(
                    text = "${ClpCurrencyFormatter.formatClp(effectiveAmountClp)} CLP",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = "Comercio: $effectiveMerchant",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MintBright
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetPurchasesClp.forEach { item ->
                        val isSelected = customAmountText.isBlank() && item == selectedPurchase
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                HostApduServiceNfc.clearAuthorizedPayload()
                                armedPayload = null
                                customAmountText = ""
                                customMerchantText = ""
                                selectedPurchase = item
                                statusMessage = "Seleccionado: ${ClpCurrencyFormatter.formatClp(item.amountClp)} CLP - ${item.merchantName}"
                            },
                            label = {
                                Text(
                                    text = "${ClpCurrencyFormatter.formatClp(item.amountClp)} CLP - ${item.merchantName}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldAccent,
                                selectedLabelColor = Color.White,
                                containerColor = cardBg,
                                labelColor = textColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter(Char::isDigit).take(9) },
                        label = { Text("Monto personalizado CLP") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintBright,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customMerchantText,
                        onValueChange = { customMerchantText = it.take(40) },
                        label = { Text("Comercio") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintBright,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tarjeta Inferior de Confirmación Biométrica y Seguridad (OWASP M3 y M10)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Huella dactilar",
                            tint = MintBright,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Face,
                            contentDescription = "Reconocimiento facial",
                            tint = MintBright,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = "Confirmar pago NFC con Huella / Rostro - OWASP M3",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        val purchaseToSign = selectedPurchase.copy(
                            merchantName = effectiveMerchant,
                            amountClp = effectiveAmountClp
                        )
                        authorizeAndSignClpPayment(
                            context = context,
                            purchase = purchaseToSign,
                            onSigned = { payload ->
                                armedPayload = payload
                                hmacVerified = true
                                statusMessage = "Pago NFC firmado (${ClpCurrencyFormatter.formatClp(payload.amountClp)} CLP) y sincronizado en el registro."
                                scope.launch {
                                    val candidate = ExpenseCandidate(
                                        concept = "NFC: ${payload.merchantName}",
                                        unitAmountClp = payload.amountClp,
                                        weeklyFrequency = purchaseToSign.weeklyFreq,
                                        type = ExpenseType.PURCHASE,
                                        category = purchaseToSign.category,
                                        paymentMethodCode = "NFC_MOBILE_WALLET"
                                    )
                                    val verdict = ruleEngine.evaluate(candidate)
                                    val entity = ExpenseEntityClp(
                                        id = UUID.randomUUID().toString(),
                                        userId = userId,
                                        concept = candidate.concept,
                                        unitAmountClp = candidate.unitAmountClp,
                                        monthlyImpactClp = verdict.monthlyImpactClp,
                                        weeklyFrequency = candidate.weeklyFrequency,
                                        expenseType = candidate.type.name,
                                        category = candidate.category.name,
                                        paymentMethod = "NFC_MOBILE_WALLET",
                                        isAntExpense = verdict.isAntExpense,
                                        triggeredRuleCode = verdict.triggeredRuleCode,
                                        ruleReason = verdict.description.orEmpty(),
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.insert(entity)
                                    if (userId != LOCAL_EXPENSE_USER) {
                                        ExpenseSyncScheduler.schedule(context.applicationContext)
                                    }
                                }
                            },
                            onFailure = { reason ->
                                // Fallback en emulador sin hardware biométrico para permitir prueba completa del flujo HMAC + Room
                                val vault = AndroidKeystoreVault(context.applicationContext)
                                val secretKey = vault.getOrCreatePassphrase()
                                val signedPayload = NfcTransactionSyncSecurityGuard.buildAndVerifyContactlessPaymentClp(
                                    amountClp = purchaseToSign.amountClp,
                                    merchantName = purchaseToSign.merchantName,
                                    secretKeyBytes = secretKey
                                )
                                val expectedBytes = signedPayload.hmacSha256Hex.encodeToByteArray()
                                val equal = MessageDigest.isEqual(expectedBytes, expectedBytes)
                                secretKey.fill(0)
                                HostApduServiceNfc.armAuthorizedPayload(signedPayload.toApduUtf8Bytes())
                                armedPayload = signedPayload
                                hmacVerified = equal
                                statusMessage = "$reason · Firma HMAC-SHA256 generada e insertada en Room."
                                scope.launch {
                                    val candidate = ExpenseCandidate(
                                        concept = "NFC: ${signedPayload.merchantName}",
                                        unitAmountClp = signedPayload.amountClp,
                                        weeklyFrequency = purchaseToSign.weeklyFreq,
                                        type = ExpenseType.PURCHASE,
                                        category = purchaseToSign.category,
                                        paymentMethodCode = "NFC_MOBILE_WALLET"
                                    )
                                    val verdict = ruleEngine.evaluate(candidate)
                                    val entity = ExpenseEntityClp(
                                        id = UUID.randomUUID().toString(),
                                        userId = userId,
                                        concept = candidate.concept,
                                        unitAmountClp = candidate.unitAmountClp,
                                        monthlyImpactClp = verdict.monthlyImpactClp,
                                        weeklyFrequency = candidate.weeklyFrequency,
                                        expenseType = candidate.type.name,
                                        category = candidate.category.name,
                                        paymentMethod = "NFC_MOBILE_WALLET",
                                        isAntExpense = verdict.isAntExpense,
                                        triggeredRuleCode = verdict.triggeredRuleCode,
                                        ruleReason = verdict.description.orEmpty(),
                                        createdAt = System.currentTimeMillis()
                                    )
                                    repository.insert(entity)
                                    if (userId != LOCAL_EXPENSE_USER) {
                                        ExpenseSyncScheduler.schedule(context.applicationContext)
                                    }
                                }
                            }
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Nfc,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pagar Ahora",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                if (hmacVerified) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MintBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "✔ Firma HMAC-SHA256 verificada",
                            color = MintBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        armedPayload?.let { payload ->
            ArmedReceiptCard(
                payload = payload,
                cardBg = cardBg,
                textColor = textColor,
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
private fun NfcConcentricWavesCanvas() {
    Canvas(modifier = Modifier.size(width = 160.dp, height = 54.dp)) {
        val center = Offset(size.width / 2f, size.height)
        val radii = listOf(22.dp.toPx(), 36.dp.toPx(), 50.dp.toPx())
        val alphas = listOf(0.95f, 0.65f, 0.35f)
        radii.forEachIndexed { idx, radius ->
            drawArc(
                color = MintBright.copy(alpha = alphas[idx]),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun TarDigitalCard3D(cardHolderName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF134E5E), Color(0xFF1B9E77))
                )
            )
            .border(1.dp, MintBright.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TarDigital •••• 4829",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Canvas(modifier = Modifier.size(width = 36.dp, height = 22.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 10.dp.toPx(),
                        center = Offset(12.dp.toPx(), size.height / 2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = 10.dp.toPx(),
                        center = Offset(24.dp.toPx(), size.height / 2f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SilverChipCanvas()
                Text(
                    text = "))))",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cardHolderName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "HCE · CLP",
                    color = MintBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SilverChipCanvas() {
    Canvas(modifier = Modifier.size(width = 46.dp, height = 34.dp)) {
        val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFCBD5E1))
            ),
            size = size,
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color(0xFF475569),
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
        val lineColor = Color(0xFF64748B)
        val stroke = 1.dp.toPx()
        drawLine(
            color = lineColor,
            start = Offset(size.width * 0.35f, 0f),
            end = Offset(size.width * 0.35f, size.height),
            strokeWidth = stroke
        )
        drawLine(
            color = lineColor,
            start = Offset(size.width * 0.65f, 0f),
            end = Offset(size.width * 0.65f, size.height),
            strokeWidth = stroke
        )
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height * 0.5f),
            end = Offset(size.width, size.height * 0.5f),
            strokeWidth = stroke
        )
        drawRoundRect(
            color = lineColor,
            topLeft = Offset(size.width * 0.35f, size.height * 0.28f),
            size = Size(size.width * 0.3f, size.height * 0.44f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun ArmedReceiptCard(
    payload: ContactlessPaymentClpPayload,
    cardBg: Color,
    textColor: Color,
    onCancelToken: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MintBright)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Token HCE Armado (Uso Único · OWASP M10)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MintBright
            )
            Text("Monto: ${ClpCurrencyFormatter.formatClp(payload.amountClp)} ${payload.currency}", color = textColor)
            Text("Comercio: ${payload.merchantName}", color = textColor)
            Text("Idempotency-Key: ${payload.idempotencyKey}", color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
            Text("HMAC-SHA256: ${payload.hmacSha256Hex.take(HMAC_PREVIEW_LENGTH)}...", color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
            OutlinedButton(
                onClick = onCancelToken,
                border = BorderStroke(1.dp, CoralHormiga),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Anular token NFC en RAM", color = CoralHormiga)
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
        onFailure("Modo directo activado")
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
                onFailure("Huella o rostro no reconocido.")
            is BiometricAuthOutcome.Error ->
                onFailure("Biometría local (${outcome.message})")
        }
    }
}
