package cl.finantzas.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.FinancialThresholds
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.ui.viewmodel.BudgetViewModel
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    embedded: Boolean = false
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val cards by viewModel.cards.collectAsState()
    val status by viewModel.status.collectAsState()
    val format = remember { ClpCurrencyFormatter }

    val modifier = if (embedded) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Alertas y Presupuestos (CLP)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        status?.let {
            Text(it, color = MintBright, style = MaterialTheme.typography.bodySmall)
        }
        cards.forEach { card ->
            var input by remember(card.category, card.limit) { mutableStateOf(card.limit.toString()) }
            val color = when {
                card.percentage >= FinancialThresholds.BUDGET_CRITICAL_PERCENTAGE -> CoralHormiga
                card.percentage >= FinancialThresholds.BUDGET_WARNING_PERCENTAGE -> Color(0xFFF6AA1C)
                else -> MintBright
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = String.format(Locale.forLanguageTag("es-CL"), "%.0f%% consumido", card.percentage),
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Gastado: ${format.formatClp(card.spent)} / Límite: ${format.formatClp(card.limit)} CLP",
                        color = textColor.copy(alpha = 0.8f)
                    )
                    LinearProgressIndicator(
                        progress = { card.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = color
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.filter(Char::isDigit) },
                            label = { Text("Límite mensual CLP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.save(card.category, input) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
