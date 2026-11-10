package cl.finantzas.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBg
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import kotlin.math.roundToInt

private const val MONTHS_IN_YEAR = 12
private const val PERCENT_SCALE = 100L
private const val DEFAULT_DEMO_ANT_CLP = 70_000L

private val presetPercents = listOf(25, 50, 100)
private val curveColors = listOf(
    25 to Color(0xFF38A3A5),
    50 to MintBright,
    100 to CoralHormiga
)

/** Proyección orientativa calculada desde los gastos hormiga mensuales del usuario en CLP sin decimales. */
@Composable
fun SavingsSimulatorScreen(
    viewModel: ExpenseViewModel,
    embedded: Boolean = false
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val antMonthlyClp by viewModel.antMonthlyClp.collectAsState()
    var selectedPercent by rememberSaveable { mutableIntStateOf(50) }
    var projectedMonths by rememberSaveable { mutableIntStateOf(MONTHS_IN_YEAR) }
    var quickActionsEnabled by rememberSaveable { mutableStateOf(true) }

    val effectiveBaseClp = if (antMonthlyClp > 0L) antMonthlyClp else DEFAULT_DEMO_ANT_CLP
    val monthlySavingsClp = savingsPerMonth(effectiveBaseClp, selectedPercent)
    val projectedSavingsClp = monthlySavingsClp * projectedMonths
    val annualSavingsClp = monthlySavingsClp * MONTHS_IN_YEAR

    val width25 by animateFloatAsState(if (selectedPercent == 25) 4.5f else 2f, label = "c25")
    val width50 by animateFloatAsState(if (selectedPercent == 50) 4.5f else 2f, label = "c50")
    val width100 by animateFloatAsState(if (selectedPercent == 100) 4.5f else 2f, label = "c100")
    val strokeWidths = listOf(width25, width50, width100)

    val containerModifier = if (embedded) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    }

    Card(
        modifier = containerModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Simulador de Ahorro",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )

            SavingsPillSelector(
                selectedPercent = selectedPercent,
                isDark = isDark,
                borderColor = borderColor,
                textColor = textColor,
                onSelect = { percent -> selectedPercent = percent }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick actions",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = quickActionsEnabled,
                    onCheckedChange = { enabled ->
                        quickActionsEnabled = enabled
                        if (enabled && selectedPercent !in presetPercents) {
                            selectedPercent = 50
                        }
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

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Meta Mensual (% de reducción)",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$selectedPercent%",
                        color = MintBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = selectedPercent.toFloat(),
                    onValueChange = { value ->
                        selectedPercent = value.roundToInt().coerceIn(10, 100)
                    },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MintBright,
                        inactiveTrackColor = if (isDark) DarkPetrolBg else Color(0xFFCBD5E1)
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Plazo Estimado (Meses)",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$projectedMonths meses",
                        color = MintBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = projectedMonths.toFloat(),
                    onValueChange = { value ->
                        projectedMonths = value.roundToInt().coerceIn(1, MONTHS_IN_YEAR)
                    },
                    valueRange = 1f..MONTHS_IN_YEAR.toFloat(),
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MintBright,
                        inactiveTrackColor = if (isDark) DarkPetrolBg else Color(0xFFCBD5E1)
                    )
                )
            }

            // Gráfico multicurva en Canvas (Mes 1 a Mes 12) sobre cuadrícula sutil
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Proyección Acumulada (Mes 1 a Mes $projectedMonths)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                val maxAnnual = (effectiveBaseClp * MONTHS_IN_YEAR).coerceAtLeast(1L)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .semantics {
                            contentDescription = "Gráfico multicurva de ahorro acumulado de 1 a 12 meses en CLP"
                        }
                ) {
                    val gW = size.width
                    val gH = size.height - 6.dp.toPx()
                    val gridColor = textColor.copy(alpha = 0.14f)

                    for (row in 0..3) {
                        val y = gH * (row / 3f)
                        drawLine(gridColor, Offset(0f, y), Offset(gW, y), strokeWidth = 1.dp.toPx())
                    }
                    for (col in 0..3) {
                        val x = gW * (col / 3f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, gH), strokeWidth = 1.dp.toPx())
                    }

                    curveColors.forEachIndexed { idx, (pct, color) ->
                        val mSave = savingsPerMonth(effectiveBaseClp, pct)
                        val path = Path()
                        for (m in 1..projectedMonths.coerceAtLeast(2)) {
                            val x = gW * (m - 1).toFloat() / (MONTHS_IN_YEAR - 1).toFloat()
                            val fraction = ((mSave * m).toDouble() / maxAnnual.toDouble()).toFloat().coerceIn(0f, 1f)
                            val y = gH * (1f - fraction)
                            if (m == 1) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = strokeWidths[idx].dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mes 1", fontSize = 10.sp, color = textColor.copy(alpha = 0.65f))
                    Text("Mes 6", fontSize = 10.sp, color = textColor.copy(alpha = 0.65f))
                    Text("Mes 12", fontSize = 10.sp, color = textColor.copy(alpha = 0.65f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    curveColors.forEach { (pct, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("$pct%", fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Ahorro mensual ($selectedPercent%):",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Proyección ($projectedMonths m / Anual):",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${ClpCurrencyFormatter.formatClp(monthlySavingsClp)} CLP",
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${ClpCurrencyFormatter.formatClp(projectedSavingsClp)} / ${ClpCurrencyFormatter.formatClp(annualSavingsClp)} CLP",
                        color = MintBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

internal fun savingsPerMonth(monthlyBase: Long, percent: Int): Long =
    (monthlyBase / PERCENT_SCALE) * percent + (monthlyBase % PERCENT_SCALE) * percent / PERCENT_SCALE

@Composable
private fun SavingsPillSelector(
    selectedPercent: Int,
    isDark: Boolean,
    borderColor: Color,
    textColor: Color,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presetPercents.forEach { percent ->
            val isSelected = selectedPercent == percent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) EmeraldAccent
                        else if (isDark) DarkPetrolBg else Color(0xFFE2E8F0)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MintBright else borderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(percent) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$percent%",
                    color = if (isSelected) Color.White else textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
