package cl.finantzas.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.ExpenseType
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBg
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import kotlinx.coroutines.launch

private val expenseCategories = listOf(
    ExpenseCategory.MICRO_PURCHASES_AND_SNACKS to "Micro-compras y snacks",
    ExpenseCategory.FOOD_AND_GROCERIES to "Alimentación y supermercado",
    ExpenseCategory.HOUSING_AND_UTILITIES to "Vivienda y servicios",
    ExpenseCategory.TRANSPORT_AND_MOBILITY to "Transporte y movilidad",
    ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS to "Suscripciones entretenimiento"
)

private val paymentMethods = listOf(
    "CREDIT_CARD" to "Crédito",
    "DEBIT_CARD" to "Débito",
    "CASH" to "Efectivo",
    "BANK_TRANSFER" to "Transferencia",
    "NFC_MOBILE_WALLET" to "Billetera NFC"
)

private val frequencies = (0..7).map { frequency ->
    frequency.toString() to if (frequency == 0) "Mensual fijo (Suscripción)" else "$frequency veces/sem"
}

/** Formulario de gasto en CLP con cálculo del motor de reglas (RN-01, RN-02, RN-03) antes de guardar. */
@Composable
fun ExpenseRegistrationForm(
    onSave: suspend (ExpenseCandidate, manuallyConfirmedAsAnt: Boolean) -> String?
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val scope = rememberCoroutineScope()
    val ruleEngine = remember { AntExpenseRuleEngine() }

    var concept by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("3500") }
    var frequencyCode by remember { mutableStateOf("4") }
    var category by remember { mutableStateOf(ExpenseCategory.MICRO_PURCHASES_AND_SNACKS) }
    var paymentMethod by remember { mutableStateOf("DEBIT_CARD") }
    var userTouchedAntSwitch by remember { mutableStateOf(false) }
    var isAntSwitchChecked by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toLongOrNull() ?: 0L
    val frequency = frequencyCode.toIntOrNull() ?: 0
    val type = if (frequency == 0 || category == ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS) {
        ExpenseType.SUBSCRIPTION
    } else {
        ExpenseType.PURCHASE
    }

    val candidate = remember(concept, amount, frequency, type, category, paymentMethod) {
        ExpenseCandidate(
            concept = concept.trim().ifBlank { "Ej. Café Grano" },
            unitAmountClp = amount,
            weeklyFrequency = frequency,
            type = type,
            category = category,
            paymentMethodCode = paymentMethod
        )
    }
    val verdict = remember(candidate) { ruleEngine.evaluate(candidate) }

    // Pre-activar automáticamente si el motor detecta RN-02 o RN-03, permitiendo override manual RN-01
    LaunchedEffect(verdict.isAntExpense, verdict.triggeredRuleCode) {
        if (!userTouchedAntSwitch) {
            isAntSwitchChecked = verdict.isAntExpense
        }
    }

    val effectiveRuleCode = when {
        isAntSwitchChecked && verdict.isAntExpense -> verdict.triggeredRuleCode
        isAntSwitchChecked && !verdict.isAntExpense -> "RN-01"
        else -> "ESSENTIAL"
    }
    val valid = concept.trim().length >= 2 && amount > 0L

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MintBright,
        unfocusedBorderColor = borderColor,
        focusedLabelColor = MintBright,
        unfocusedLabelColor = textColor.copy(alpha = 0.7f),
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        cursorColor = MintBright
    )

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
                text = "Registrar Nuevo Gasto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            OutlinedTextField(
                value = concept,
                onValueChange = { concept = it.take(80) },
                label = { Text("Concepto") },
                placeholder = { Text("Ej. Café Grano", color = textColor.copy(alpha = 0.45f)) },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(12) },
                    label = { Text("Monto ($)") },
                    prefix = { Text("$ ", color = MintBright, fontWeight = FontWeight.Bold) },
                    suffix = { Text("CLP", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp) },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1.2f)
                )

                ChoiceDropdown(
                    label = "Frecuencia",
                    selectedLabel = frequencies.first { it.first == frequencyCode }.second,
                    options = frequencies,
                    onSelected = {
                        frequencyCode = it
                        userTouchedAntSwitch = false
                    },
                    borderColor = borderColor,
                    textColor = textColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChoiceDropdown(
                    label = "Categoría",
                    selectedLabel = expenseCategories.first { it.first == category }.second,
                    options = expenseCategories.map { it.first.name to it.second },
                    onSelected = { selected ->
                        category = ExpenseCategory.valueOf(selected)
                        userTouchedAntSwitch = false
                    },
                    borderColor = borderColor,
                    textColor = textColor,
                    modifier = Modifier.weight(1f)
                )

                ChoiceDropdown(
                    label = "Medio de Pago",
                    selectedLabel = paymentMethods.first { it.first == paymentMethod }.second,
                    options = paymentMethods,
                    onSelected = { paymentMethod = it },
                    borderColor = borderColor,
                    textColor = textColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿Es Gasto Hormiga? ($effectiveRuleCode)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAntSwitchChecked) CoralHormiga else textColor
                    )
                    Text(
                        text = "Impacto mensual: ${ClpCurrencyFormatter.formatClp(verdict.monthlyImpactClp)} CLP/mes",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.75f)
                    )
                }
                Switch(
                    checked = isAntSwitchChecked,
                    onCheckedChange = { checked ->
                        userTouchedAntSwitch = true
                        isAntSwitchChecked = checked
                        message = null
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldAccent,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = if (isDark) DarkPetrolBg else Color(0xFFCBD5E1),
                        uncheckedBorderColor = borderColor
                    )
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        message = null
                        val finalCandidate = candidate.copy(concept = concept.trim())
                        val result = try {
                            onSave(finalCandidate, isAntSwitchChecked)
                        } catch (error: Exception) {
                            error.message ?: "No se pudo guardar el gasto."
                        }
                        message = result ?: "Gasto registrado en CLP."
                        if (result == null) {
                            concept = ""
                            amountText = ""
                            userTouchedAntSwitch = false
                        }
                        isSaving = false
                    }
                },
                enabled = valid && !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = Color.White,
                    disabledContainerColor = borderColor,
                    disabledContentColor = textColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isSaving) "Guardando…" else "+ Registrar Gasto",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (message != null) {
                Text(
                    text = message.orEmpty(),
                    color = if (message == "Gasto registrado en CLP.") MintBright else CoralHormiga,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChoiceDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.75f))
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = selectedLabel,
                    maxLines = 1,
                    color = textColor,
                    fontSize = 12.sp
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, title) ->
                    DropdownMenuItem(
                        text = { Text(title) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
