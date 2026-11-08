package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter

internal data class ExpenseFilterOption(
    val key: String,
    val label: String
)

private val expenseFilterOptions = listOf(
    ExpenseFilterOption("ALL", "Todos"),
    ExpenseFilterOption("ANT", "Gastos hormiga"),
    ExpenseFilterOption("SUBSCRIPTIONS", "Suscripciones"),
    ExpenseFilterOption("PAYMENT:CREDIT_CARD", "Pago · Crédito"),
    ExpenseFilterOption("PAYMENT:DEBIT_CARD", "Pago · Débito"),
    ExpenseFilterOption("PAYMENT:CASH", "Pago · Efectivo"),
    ExpenseFilterOption("PAYMENT:BANK_TRANSFER", "Pago · Transferencia"),
    ExpenseFilterOption("PAYMENT:NFC_MOBILE_WALLET", "Pago · NFC")
)

@Composable
internal fun ExpenseMovementFilterChips(
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(expenseFilterOptions, key = ExpenseFilterOption::key) { option ->
            FilterChip(
                selected = selectedKey == option.key,
                onClick = { onSelected(option.key) },
                label = { Text(option.label) }
            )
        }
    }
}

@Composable
internal fun ExpenseMovementCard(
    expense: ExpenseEntityClp,
    onToggleClassification: () -> Unit,
    onDelete: () -> Unit
) {
    val typeLabel = when (expense.expenseType) {
        "SUBSCRIPTION" -> "Suscripción"
        "FIXED_PAYMENT", "FIXED" -> "Pago fijo"
        else -> "Compra"
    }
    val categoryLabel = expense.category
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar(Char::uppercase)
    val paymentLabel = when (expense.paymentMethod) {
        "CREDIT_CARD" -> "Tarjeta de crédito"
        "DEBIT_CARD", "DEBIT" -> "Tarjeta de débito"
        "CASH" -> "Efectivo"
        "BANK_TRANSFER" -> "Transferencia"
        "NFC_MOBILE_WALLET" -> "Billetera NFC"
        else -> expense.paymentMethod
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.concept, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$typeLabel · ${ClpCurrencyFormatter.formatClp(expense.unitAmountClp)} por movimiento",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${expense.concept}")
                }
            }

            Text(
                "Impacto mensual: ${ClpCurrencyFormatter.formatClp(expense.monthlyImpactClp)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("$categoryLabel · $paymentLabel", style = MaterialTheme.typography.bodySmall)
            Text(
                if (expense.weeklyFrequency == 0) "Frecuencia: mensual fijo"
                else "Frecuencia: ${expense.weeklyFrequency} veces por semana",
                style = MaterialTheme.typography.bodySmall
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (expense.isAntExpense) "Gasto hormiga · ${expense.triggeredRuleCode}"
                    else "Gasto esencial",
                    color = if (expense.isAntExpense) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onToggleClassification) {
                    Text(if (expense.isAntExpense) "Marcar esencial" else "Marcar hormiga")
                }
            }
        }
    }
}

internal fun filterExpenses(
    expenses: List<ExpenseEntityClp>,
    filterKey: String
): List<ExpenseEntityClp> = when {
    filterKey == "ANT" -> expenses.filter(ExpenseEntityClp::isAntExpense)
    filterKey == "SUBSCRIPTIONS" -> expenses.filter { it.expenseType == "SUBSCRIPTION" }
    filterKey.startsWith("PAYMENT:") -> {
        val method = filterKey.substringAfter(':')
        expenses.filter { normalizePaymentMethod(it.paymentMethod) == method }
    }
    else -> expenses
}

private fun normalizePaymentMethod(method: String): String = when (method) {
    "DEBIT" -> "DEBIT_CARD"
    "CREDIT" -> "CREDIT_CARD"
    else -> method
}
