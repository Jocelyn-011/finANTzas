package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.ExpenseCandidate
import cl.aiep.finantzas.domain.rules.ExpenseCategory
import cl.aiep.finantzas.domain.rules.ExpenseType
import kotlinx.coroutines.launch

private val expenseTypes = listOf(
    ExpenseType.PURCHASE to "Compra",
    ExpenseType.SUBSCRIPTION to "Suscripción"
)

private val expenseCategories = listOf(
    ExpenseCategory.MICRO_PURCHASES_AND_SNACKS to "Microcompras y snacks",
    ExpenseCategory.FOOD_AND_GROCERIES to "Alimentación y supermercado",
    ExpenseCategory.ENTERTAINMENT_SUBSCRIPTIONS to "Suscripciones de entretenimiento"
)

private val paymentMethods = listOf(
    "CREDIT_CARD" to "Tarjeta de crédito",
    "DEBIT_CARD" to "Tarjeta de débito",
    "CASH" to "Efectivo",
    "BANK_TRANSFER" to "Transferencia",
    "NFC_MOBILE_WALLET" to "Billetera NFC"
)

private val frequencies = (0..7).map { frequency ->
    frequency.toString() to if (frequency == 0) "Mensual fijo" else "$frequency veces por semana"
}

/** Formulario de gasto en CLP con cálculo del motor de reglas antes de guardar. */
@Composable
fun ExpenseRegistrationForm(
    onSave: suspend (ExpenseCandidate, manuallyConfirmedAsAnt: Boolean) -> String?
) {
    val scope = rememberCoroutineScope()
    val ruleEngine = remember { AntExpenseRuleEngine() }

    var concept by remember { mutableStateOf("Café") }
    var amountText by remember { mutableStateOf("3500") }
    var frequencyCode by remember { mutableStateOf("4") }
    var type by remember { mutableStateOf(ExpenseType.PURCHASE) }
    var category by remember { mutableStateOf(ExpenseCategory.MICRO_PURCHASES_AND_SNACKS) }
    var paymentMethod by remember { mutableStateOf("DEBIT_CARD") }
    var manualConfirmation by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toLongOrNull() ?: 0L
    val frequency = frequencyCode.toIntOrNull() ?: 0
    val candidate = remember(concept, amount, frequency, type, category, paymentMethod) {
        ExpenseCandidate(
            concept = concept.trim(),
            unitAmountClp = amount,
            weeklyFrequency = frequency,
            type = type,
            category = category,
            paymentMethodCode = paymentMethod
        )
    }
    val verdict = remember(candidate) { ruleEngine.evaluate(candidate) }
    val isAntExpense = manualConfirmation || verdict.isAntExpense
    val ruleCode = if (manualConfirmation) "RN-01" else verdict.triggeredRuleCode
    val valid = concept.trim().length >= 2 && amount > 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Registrar gasto", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = concept,
                onValueChange = { concept = it.take(80) },
                label = { Text("Concepto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(12) },
                label = { Text("Monto por movimiento (CLP)") },
                prefix = { Text("$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceDropdown(
                    label = "Tipo",
                    selectedLabel = expenseTypes.first { it.first == type }.second,
                    options = expenseTypes.map { it.first.name to it.second },
                    onSelected = { selected -> type = ExpenseType.valueOf(selected) },
                    modifier = Modifier.weight(1f)
                )
                ChoiceDropdown(
                    label = "Frecuencia",
                    selectedLabel = frequencies.first { it.first == frequencyCode }.second,
                    options = frequencies,
                    onSelected = { frequencyCode = it },
                    modifier = Modifier.weight(1f)
                )
            }

            ChoiceDropdown(
                label = "Categoría",
                selectedLabel = expenseCategories.first { it.first == category }.second,
                options = expenseCategories.map { it.first.name to it.second },
                onSelected = { selected -> category = ExpenseCategory.valueOf(selected) },
                modifier = Modifier.fillMaxWidth()
            )

            ChoiceDropdown(
                label = "Medio de pago",
                selectedLabel = paymentMethods.first { it.first == paymentMethod }.second,
                options = paymentMethods,
                onSelected = { paymentMethod = it },
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAntExpense) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Diagnóstico en vivo · $ruleCode", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Impacto mensual: ${ClpCurrencyFormatter.formatClp(verdict.monthlyImpactClp)} / mes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (manualConfirmation) {
                            "Gasto hormiga confirmado manualmente (RN-01)."
                        } else if (verdict.isAntExpense) {
                            verdict.description.orEmpty()
                        } else {
                            "Clasificado como gasto esencial u ordinario."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Confirmación manual (RN-01)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Marcar como gasto hormiga aunque el motor no lo detecte",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = manualConfirmation,
                    onCheckedChange = {
                        manualConfirmation = it
                        message = null
                    }
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        message = null
                        val result = try {
                            onSave(candidate, manualConfirmation)
                        } catch (error: Exception) {
                            error.message ?: "No se pudo guardar el gasto."
                        }
                        message = result ?: "Gasto guardado en el registro."
                        if (result == null) {
                            concept = ""
                            amountText = ""
                            manualConfirmation = false
                        }
                        isSaving = false
                    }
                },
                enabled = valid && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Guardando…" else "Guardar gasto")
            }

            if (message != null) {
                Text(
                    text = message.orEmpty(),
                    color = if (message == "Gasto guardado en el registro.") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodySmall
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.layout.Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedLabel, maxLines = 1)
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
