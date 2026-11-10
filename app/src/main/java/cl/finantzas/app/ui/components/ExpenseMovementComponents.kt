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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright

internal data class ExpenseFilterOption(
    val key: String,
    val label: String
)

private val expenseFilterOptions = listOf(
    ExpenseFilterOption("ALL", "Todos"),
    ExpenseFilterOption("ANT", "Hormiga"),
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
    val isDark = LocalFinANTzasDarkTheme.current
    val chipBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(expenseFilterOptions, key = ExpenseFilterOption::key) { option ->
            FilterChip(
                selected = selectedKey == option.key,
                onClick = { onSelected(option.key) },
                shape = RoundedCornerShape(50),
                label = { Text(option.label, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldAccent,
                    selectedLabelColor = Color.White,
                    containerColor = chipBg,
                    labelColor = textColor
                )
            )
        }
    }
}

private fun categoryIconFor(category: String): ImageVector = when (category.uppercase()) {
    "HOUSING_AND_UTILITIES", "VIVIENDA" -> Icons.Outlined.Home
    "FOOD_AND_GROCERIES", "ALIMENTACION" -> Icons.Outlined.Fastfood
    "TRANSPORT_AND_MOBILITY", "TRANSPORTE" -> Icons.Outlined.DirectionsBus
    "ENTERTAINMENT_SUBSCRIPTIONS", "ENTRETENIMIENTO" -> Icons.Outlined.Movie
    else -> Icons.Outlined.LocalCafe
}

@Composable
internal fun ExpenseMovementCard(
    expense: ExpenseEntityClp,
    onToggleClassification: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val categoryLabel = expense.category.toExpenseCategoryLabel()
    val paymentLabel = when (expense.paymentMethod) {
        "CREDIT_CARD" -> "Crédito"
        "DEBIT_CARD", "DEBIT" -> "Débito"
        "CASH" -> "Efectivo"
        "BANK_TRANSFER" -> "Transferencia"
        "NFC_MOBILE_WALLET" -> "Billetera NFC"
        else -> expense.paymentMethod
    }

    val isAnt = expense.isAntExpense
    val circleBg = if (isAnt) CoralHormiga.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.2f)
    val accentColor = if (isAnt) CoralHormiga else MintBright

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIconFor(expense.category),
                        contentDescription = categoryLabel,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = expense.concept,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "$categoryLabel · $paymentLabel",
                        color = textColor.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Píldora interactiva ("Hormiga" o "Esencial") que cambia la clasificación al tocarla
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(circleBg)
                            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable { onToggleClassification() }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isAnt) "Hormiga (${expense.triggeredRuleCode.ifBlank { "RN-01" }})" else "Esencial",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${ClpCurrencyFormatter.formatClp(expense.monthlyImpactClp)} CLP",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (expense.weeklyFrequency == 0) {
                            "Mensual fijo"
                        } else {
                            "${expense.weeklyFrequency}x/sem (${ClpCurrencyFormatter.formatClp(expense.unitAmountClp)})"
                        },
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar ${expense.concept}",
                        tint = textColor.copy(alpha = 0.7f)
                    )
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
    filterKey == "SUBSCRIPTIONS" -> expenses.filter {
        it.expenseType == "SUBSCRIPTION" || it.category == "ENTERTAINMENT_SUBSCRIPTIONS"
    }
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
