package cl.finantzas.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.data.remote.FinantzasAuthRepository
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBg
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.FinANTzasBrandLogo
import cl.finantzas.app.ui.theme.FinANTzasThemeState
import cl.finantzas.app.ui.theme.FondoClaro
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.ui.theme.TextoOscuro
import cl.finantzas.app.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class SettingsSheetSection {
    EDIT_PROFILE,
    NOTIFICATIONS,
    ABOUT,
    HELP_FEEDBACK,
    PRIVACY_POLICY,
    TERMS_OF_USE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    budgetViewModel: BudgetViewModel,
    onSignedOut: () -> Unit = {}
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val authRepository = remember { FinantzasAuthRepository() }
    val userEmail = remember { authRepository.currentUserEmail() ?: "usuario@finantzas.cl" }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val budgetCards by budgetViewModel.cards.collectAsState()
    val globalBudgetClp = budgetCards.firstOrNull { it.category == "GLOBAL" }?.limit ?: 950_000L

    var activeSheet by remember { mutableStateOf<SettingsSheetSection?>(null) }
    var loggingOut by remember { mutableStateOf(false) }

    // Estados persistentes de Notificaciones
    var preventiveAlertsEnabled by rememberSaveable { mutableStateOf(true) }
    var criticalAlertsEnabled by rememberSaveable { mutableStateOf(true) }
    var antMotorAlertsEnabled by rememberSaveable { mutableStateOf(true) }
    var showBudgetEditorInSheet by remember { mutableStateOf(false) }

    // Estados de Ayuda y Comentarios
    var starRating by rememberSaveable { mutableIntStateOf(5) }
    var feedbackText by rememberSaveable { mutableStateOf("") }
    val savedFeedbackNotes = remember { mutableStateListOf<String>() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Ajustes y Configuración",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Tarjeta 1: Perfil de la cuenta
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Perfil de la cuenta",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                                .border(2.dp, MintBright, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = FinANTzasThemeState.userDisplayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Text(
                                text = userEmail,
                                fontSize = 12.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Presupuesto Base: ${ClpCurrencyFormatter.formatClp(globalBudgetClp)} CLP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MintBright
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeSheet = SettingsSheetSection.EDIT_PROFILE },
                            border = BorderStroke(1.dp, MintBright),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Editar Perfil / CLP", color = MintBright, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                loggingOut = true
                                scope.launch {
                                    try {
                                        authRepository.logout()
                                        onSignedOut()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        snackbarHostState.showSnackbar("Sesión local cerrada.")
                                        onSignedOut()
                                    } finally {
                                        loggingOut = false
                                    }
                                }
                            },
                            enabled = !loggingOut,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralHormiga,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (loggingOut) "Saliendo..." else "Cerrar sesión",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Tarjeta 2: Apariencia: Claro u Oscuro (Conmutación en tiempo real)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Apariencia: Claro u Oscuro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Miniatura Claro
                        ThemePreviewMiniatureCard(
                            title = "Claro",
                            selected = !FinANTzasThemeState.isDarkTheme,
                            previewBg = FondoClaro,
                            previewCard = Color.White,
                            previewText = TextoOscuro,
                            onClick = { FinANTzasThemeState.isDarkTheme = false },
                            modifier = Modifier.weight(1f)
                        )
                        // Miniatura Oscuro Petrol
                        ThemePreviewMiniatureCard(
                            title = "Oscuro Petrol",
                            selected = FinANTzasThemeState.isDarkTheme,
                            previewBg = DarkPetrolBg,
                            previewCard = DarkPetrolCard,
                            previewText = Color.White,
                            onClick = { FinANTzasThemeState.isDarkTheme = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tarjeta 3: Menú de Opciones con Íconos Esmeralda y Chevron >
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsMenuRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notificaciones",
                        subtitle = "Alertas ≥75%, ≥100% y Motor Hormiga (RN-02/RN-03)",
                        textColor = textColor,
                        onClick = {
                            showBudgetEditorInSheet = false
                            activeSheet = SettingsSheetSection.NOTIFICATIONS
                        }
                    )
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                    SettingsMenuRow(
                        icon = Icons.Outlined.Info,
                        title = "Acerca de",
                        subtitle = "v1.0.0-mvp · Equipo AIEP y Arquitectura",
                        textColor = textColor,
                        onClick = { activeSheet = SettingsSheetSection.ABOUT }
                    )
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                    SettingsMenuRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = "Ayuda y comentarios",
                        subtitle = "Preguntas frecuentes (FAQ), calificación y soporte",
                        textColor = textColor,
                        onClick = { activeSheet = SettingsSheetSection.HELP_FEEDBACK }
                    )
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                    SettingsMenuRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "Política de privacidad",
                        subtitle = "Ley N° 19.628 / N° 21.719, SQLCipher AES-256 y RLS",
                        textColor = textColor,
                        onClick = { activeSheet = SettingsSheetSection.PRIVACY_POLICY }
                    )
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                    SettingsMenuRow(
                        icon = Icons.Outlined.Description,
                        title = "Condiciones de uso",
                        subtitle = "Términos en CLP, Ley Fintec N° 21.521 y AIEP",
                        textColor = textColor,
                        onClick = { activeSheet = SettingsSheetSection.TERMS_OF_USE }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ModalBottomSheet para cada sección 100% completa y funcional
    activeSheet?.let { section ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = cardBg,
            contentColor = textColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (section) {
                    SettingsSheetSection.EDIT_PROFILE -> {
                        var nameInput by remember { mutableStateOf(FinANTzasThemeState.userDisplayName) }
                        var budgetInput by remember { mutableStateOf(globalBudgetClp.toString()) }

                        Text("Editar Perfil y Presupuesto Base (CLP)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it.take(40) },
                            label = { Text("Nombre del titular") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it.filter(Char::isDigit).take(10) },
                            label = { Text("Presupuesto Mensual Global ($ CLP)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    FinANTzasThemeState.userDisplayName = nameInput.trim()
                                }
                                budgetViewModel.save("GLOBAL", budgetInput)
                                activeSheet = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Perfil y presupuesto CLP actualizados.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                        }
                    }

                    SettingsSheetSection.NOTIFICATIONS -> {
                        Text("Configuración de Notificaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Administra los umbrales de alerta en Pesos Chilenos (CLP) y los avisos del motor de reglas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.75f)
                        )

                        NotificationSwitchRow(
                            title = "Alertas preventivas de presupuesto (≥ 75%)",
                            subtitle = "Notifica cuando una categoría alcanza el umbral preventivo.",
                            checked = preventiveAlertsEnabled,
                            onCheckedChange = { preventiveAlertsEnabled = it },
                            textColor = textColor
                        )
                        NotificationSwitchRow(
                            title = "Alertas críticas de presupuesto excedido (≥ 100%)",
                            subtitle = "Notifica inmediatamente cuando se supera el tope mensual en CLP.",
                            checked = criticalAlertsEnabled,
                            onCheckedChange = { criticalAlertsEnabled = it },
                            textColor = textColor
                        )
                        NotificationSwitchRow(
                            title = "Avisos automáticos del Motor de Gasto Hormiga (RN-02 y RN-03)",
                            subtitle = "Detecta micro-compras > 3 veces/semana y suscripciones de entretenimiento.",
                            checked = antMotorAlertsEnabled,
                            onCheckedChange = { antMotorAlertsEnabled = it },
                            textColor = textColor
                        )

                        Button(
                            onClick = { showBudgetEditorInSheet = !showBudgetEditorInSheet },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showBudgetEditorInSheet) "Ocultar topes por categoría" else "Configurar topes por categoría (CLP)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showBudgetEditorInSheet) {
                            BudgetScreen(viewModel = budgetViewModel, embedded = true)
                        }
                    }

                    SettingsSheetSection.ABOUT -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            FinANTzasBrandLogo()
                        }
                        Text(
                            text = "v1.0.0-mvp (Android Studio + Kotlin + Jetpack Compose)",
                            fontWeight = FontWeight.Bold,
                            color = MintBright,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Propósito del Sistema:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "finANTzas es una solución móvil y web diseñada para el control inteligente de gastos mensuales en Pesos Chilenos (CLP sin decimales), detección automatizada de Gastos Hormiga (reglas RN-01, RN-02, RN-03 y RN-04), simulación interactiva de ahorro anual (25%, 50%, 100%) y pagos sin contacto mediante Billetera Móvil NFC con Host Card Emulation (HCE).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Créditos del Equipo Desarrollador:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Carrera de Ingeniería / Técnico en Programación y Análisis de Sistemas — Instituto Profesional AIEP:\n• Jocelyn González\n• Enrique Ahumada\n• Álvaro Brizuela",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Arquitectura y Estándares Aplicados:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "• Clean Architecture & Patrón MVVM (StateFlow unidireccional).\n• Principios SOLID (Patrón Strategy OCP en AntExpenseRule y BudgetThresholdPolicy) & Clean Code.\n• OWASP Mobile Top 10 (M3 Biometría, M6 FLAG_SECURE, M9 SQLCipher AES-256 + Keystore, M10 HMAC-SHA256).\n• Tríada CID (Confidencialidad, Integridad y Disponibilidad Offline-First con WorkManager).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    SettingsSheetSection.HELP_FEEDBACK -> {
                        Text("Ayuda y Comentarios (FAQ)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                        FaqAccordionItem(
                            question = "¿Cómo detecta finANTzas un Gasto Hormiga con las reglas RN-01, RN-02 y RN-03?",
                            answer = "• RN-01 (Confirmación Manual): Permite marcar o desmarcar cualquier movimiento como Gasto Hormiga desde el formulario o historial.\n• RN-02 (Micro-compras frecuentes): Clasifica automáticamente compras repetidas más de 3 veces por semana (ej. café o snacks diarios).\n• RN-03 (Suscripciones de entretenimiento): Identifica cargos recurrentes mensuales de streaming o videojuegos para visibilizar su impacto anual en CLP.",
                            borderColor = borderColor,
                            textColor = textColor
                        )
                        FaqAccordionItem(
                            question = "¿Cómo funciona el Simulador 25%, 50% y 100%?",
                            answer = "El simulador toma la suma mensual de tus Gastos Hormiga en CLP y calcula cuánto ahorrarías al reducir un 25% (moderado), 50% (recomendado) o 100% (cero hormiga) durante 1 a 12 meses, utilizando aritmética entera exacta en Pesos Chilenos sin errores de punto flotante.",
                            borderColor = borderColor,
                            textColor = textColor
                        )
                        FaqAccordionItem(
                            question = "¿Cómo se protege mi tarjeta en NFC Pay con Biometría y HMAC-SHA256?",
                            answer = "Antes de armar el token HCE, se exige autenticación biométrica Clase 3 (Huella o Rostro - OWASP M3). Luego se firma el monto en CLP con una llave del Android Keystore mediante HMAC-SHA256 e Idempotency-Key única, verificada en tiempo constante con MessageDigest.isEqual (OWASP M10) y protegiendo la pantalla con FLAG_SECURE (OWASP M6).",
                            borderColor = borderColor,
                            textColor = textColor
                        )

                        Text("Califica tu experiencia en finANTzas:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (star in 1..5) {
                                IconButton(onClick = { starRating = star }) {
                                    Icon(
                                        imageVector = if (star <= starRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "$star estrellas",
                                        tint = if (star <= starRating) Color(0xFFF6AA1C) else textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it.take(300) },
                            label = { Text("Escribe tu comentario o reporte") },
                            placeholder = { Text("Ej. Excelente precisión del motor de gasto hormiga en CLP...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val entry = "[$starRating★] ${feedbackText.trim().ifBlank { "Experiencia calificada positivamente" }}"
                                savedFeedbackNotes.add(0, entry)
                                feedbackText = ""
                                scope.launch {
                                    snackbarHostState.showSnackbar("¡Gracias! Comentario guardado localmente ($starRating/5 ★).")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enviar comentario", fontWeight = FontWeight.Bold)
                        }

                        if (savedFeedbackNotes.isNotEmpty()) {
                            Text("Comentarios enviados en este dispositivo:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MintBright)
                            savedFeedbackNotes.forEach { item ->
                                Text("• $item", fontSize = 12.sp, color = textColor.copy(alpha = 0.85f))
                            }
                        }
                    }

                    SettingsSheetSection.PRIVACY_POLICY -> {
                        Text("Política de Privacidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Cumplimiento normativo conforme a la Ley N° 19.628 sobre Protección de la Vida Privada y la Ley N° 21.719 sobre Protección de Datos Personales en la República de Chile.",
                            fontWeight = FontWeight.SemiBold,
                            color = MintBright,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "(a) Datos Recolectados:\nfinANTzas procesa exclusivamente los registros de gastos en Pesos Chilenos (CLP), topes presupuestarios por categoría y el correo electrónico asociado a la cuenta del usuario autoadministrador.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(b) Cifrado Local SQLCipher AES-256 y Android Keystore:\nToda la base de datos local Room se cifra en reposo utilizando SQLCipher con algoritmo AES-256. La frase de paso criptográfica es generada y custodiada dentro del hardware seguro Android Keystore (AES/GCM/NoPadding).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(c) Aislamiento por Cuenta con Row-Level Security (RLS):\nLa sincronización en la nube con Supabase PostgreSQL aplica políticas estrictas de Row-Level Security (auth.uid() = user_id) y transporte TLS 1.3 con Network Security Config.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(d) Tokenización de Pagos NFC:\nEn cumplimiento de estándares de seguridad financiera, finANTzas jamás almacena ni transmite el número real de tarjeta (PAN). Los pagos sin contacto operan mediante tokens efímeros en memoria RAM firmados con HMAC-SHA256 de uso único.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(e) Derechos ARCO (Acceso, Rectificación, Cancelación y Oposición):\nEl titular puede consultar, modificar o eliminar de forma inmediata cualquier gasto o presupuesto desde la aplicación, ejerciendo pleno control y portabilidad sobre su información financiera.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    SettingsSheetSection.TERMS_OF_USE -> {
                        Text("Condiciones de Uso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Términos de Servicio del Ecosistema finANTzas (CLP)",
                            fontWeight = FontWeight.SemiBold,
                            color = MintBright,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "(a) Uso Personal y Educativo:\nEl sistema finANTzas se provee como herramienta de apoyo en educación financiera, control presupuestario mensual y detección inteligente de gastos hormiga en Pesos Chilenos ($ CLP sin decimales).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(b) Carácter Referencial del Simulador de Ahorro:\nLas proyecciones del Simulador de Ahorro (escenarios de reducción del 25%, 50% y 100% a 12 meses) y los semáforos de alerta preventiva (75%) y crítica (100%) constituyen estimaciones orientativas basadas en los hábitos registrados por el propio usuario.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(c) Responsabilidad del Usuario y Ley Fintec N° 21.521:\nConforme a los lineamientos de seguridad e innovación financiera de la Ley Fintec N° 21.521 en Chile, el usuario autoadministrador es responsable de la custodia de sus credenciales de acceso y del uso de los factores biométricos de su dispositivo móvil para autorizar operaciones NFC.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "(d) Propiedad Intelectual:\nDiseño, motor de reglas de negocio, arquitectura de seguridad y código fuente desarrollados por el equipo integrado por Jocelyn González, Enrique Ahumada y Álvaro Brizuela en el Instituto Profesional AIEP.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewMiniatureCard(
    title: String,
    selected: Boolean,
    previewBg: Color,
    previewCard: Color,
    previewText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(previewBg)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MintBright else DarkPetrolBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = previewText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Tema activo",
                    tint = MintBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewCard)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                )
                Text(
                    text = "$950.000 CLP",
                    color = previewText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmeraldAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MintBright,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.65f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Abrir $title",
            tint = MintBright
        )
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            Text(text = subtitle, fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldAccent
            )
        )
    }
}

@Composable
private fun FaqAccordionItem(
    question: String,
    answer: String,
    borderColor: Color,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MintBright,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "−" else "+",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MintBright
            )
        }
        if (expanded) {
            Text(
                text = answer,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}
