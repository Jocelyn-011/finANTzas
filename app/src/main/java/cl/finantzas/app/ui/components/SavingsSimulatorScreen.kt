package cl.finantzas.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel

private const val MONTHS_IN_YEAR = 12
private const val PERCENT_SCALE = 100L

private data class SavingsScenario(val percent: Int, val label: String, val color: Color)

private val savingsScenarios = listOf(
    SavingsScenario(25, "Reducir 25 %", Color(0xFF3A6DA8)),
    SavingsScenario(50, "Reducir 50 % · Recomendado", Color(0xFF008577)),
    SavingsScenario(100, "Reducir 100 % · Cero hormiga", Color(0xFFE45773))
)

/** Proyección orientativa calculada desde los gastos hormiga mensuales del usuario. */
@Composable
fun SavingsSimulatorScreen(viewModel: ExpenseViewModel) {
    val antMonthlyClp by viewModel.antMonthlyClp.collectAsState()
    var selectedPercent by rememberSaveable { mutableIntStateOf(50) }
    val monthlyBase = antMonthlyClp.coerceAtLeast(0L)

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Simulador de ahorro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Gasto hormiga mensual: " + ClpCurrencyFormatter.formatClp(monthlyBase))
        Text(
            "Elige cuánto reducir tus gastos hormiga. La proyección supone el mismo ahorro cada mes, sin intereses.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (monthlyBase > Long.MAX_VALUE / MONTHS_IN_YEAR) {
            Text("El monto es demasiado alto para calcular una proyección anual.")
            return@Column
        }

        SavingsScenarioSelector(selectedPercent) { selectedPercent = it }
        Text("Seleccionado: " + savingsScenarios.first { it.percent == selectedPercent }.label)

        if (monthlyBase == 0L) {
            Text("No hay gastos hormiga registrados. Agrega o clasifica un gasto para ver una proyección.")
        }

        savingsScenarios.forEach { scenario ->
            val monthly = savingsPerMonth(monthlyBase, scenario.percent)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedPercent == scenario.percent) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(scenario.label, fontWeight = FontWeight.SemiBold, color = scenario.color)
                    Text("Ahorro mensual: " + ClpCurrencyFormatter.formatClp(monthly))
                    Text("Ahorro anual: " + ClpCurrencyFormatter.formatClp(monthly * MONTHS_IN_YEAR))
                }
            }
        }

        SavingsProjectionChart(monthlyBase, selectedPercent) { selectedPercent = it }
        SavingsProjectionTable(savingsPerMonth(monthlyBase, selectedPercent), selectedPercent)
    }
}

internal fun savingsPerMonth(monthlyBase: Long, percent: Int): Long =
    (monthlyBase / PERCENT_SCALE) * percent + (monthlyBase % PERCENT_SCALE) * percent / PERCENT_SCALE

@Composable
private fun SavingsScenarioSelector(selectedPercent: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        savingsScenarios.forEach { scenario ->
            FilterChip(
                selected = selectedPercent == scenario.percent,
                onClick = { onSelected(scenario.percent) },
                label = { Text(scenario.percent.toString() + " %") }
            )
        }
    }
}

@Composable
private fun SavingsProjectionChart(monthlyBase: Long, selectedPercent: Int, onSelected: (Int) -> Unit) {
    val width25 by animateFloatAsState(if (selectedPercent == 25) 5f else 2f, label = "curva25")
    val width50 by animateFloatAsState(if (selectedPercent == 50) 5f else 2f, label = "curva50")
    val width100 by animateFloatAsState(if (selectedPercent == 100) 5f else 2f, label = "curva100")
    val widths = listOf(width25, width50, width100)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Proyección de ahorro acumulado", style = MaterialTheme.typography.titleMedium)
            Text("Mes 1 a mes 12", style = MaterialTheme.typography.bodySmall)
            if (monthlyBase == 0L) {
                Text("El gráfico aparecerá cuando registres gastos hormiga.")
            } else {
                Text("Cambiar escenario", style = MaterialTheme.typography.labelMedium)
                SavingsScenarioSelector(selectedPercent, onSelected)
                val annualMaximum = monthlyBase * MONTHS_IN_YEAR
                Canvas(
                    Modifier.fillMaxWidth().height(180.dp).semantics {
                        contentDescription = "Gráfico de ahorro acumulado durante 12 meses para los escenarios de 25, 50 y 100 por ciento."
                    }
                ) {
                    val graphHeight = size.height - 8.dp.toPx()
                    val graphWidth = size.width - 8.dp.toPx()
                    val axisColor = Color.Gray.copy(alpha = 0.3f)
                    for (step in 0..4) {
                        val y = graphHeight * step / 4f
                        drawLine(axisColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(graphWidth, y))
                    }
                    savingsScenarios.forEachIndexed { index, scenario ->
                        val monthly = savingsPerMonth(monthlyBase, scenario.percent)
                        val path = Path()
                        for (month in 1..MONTHS_IN_YEAR) {
                            val x = graphWidth * (month - 1) / (MONTHS_IN_YEAR - 1)
                            val fraction = (monthly * month).toDouble() / annualMaximum.toDouble()
                            val y = graphHeight * (1f - fraction.toFloat())
                            if (month == 1) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, scenario.color, style = Stroke(width = widths[index].dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mes 1", style = MaterialTheme.typography.labelSmall)
                    Text("Mes 6", style = MaterialTheme.typography.labelSmall)
                    Text("Mes 12", style = MaterialTheme.typography.labelSmall)
                }
                savingsScenarios.forEach { scenario ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(10.dp).background(scenario.color))
                        Spacer(Modifier.width(8.dp))
                        Text(scenario.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsProjectionTable(monthlySavings: Long, selectedPercent: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Proyección mensual · " + selectedPercent + " %", style = MaterialTheme.typography.titleMedium)
            ProjectionRow("Periodo", "Ahorro del mes", "Acumulado", bold = true)
            for (month in 1..MONTHS_IN_YEAR) {
                ProjectionRow("Mes " + month, ClpCurrencyFormatter.formatClp(monthlySavings), ClpCurrencyFormatter.formatClp(monthlySavings * month))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp)
            ) {
                Text("Anual", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(ClpCurrencyFormatter.formatClp(monthlySavings * MONTHS_IN_YEAR), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProjectionRow(period: String, monthly: String, total: String, bold: Boolean = false) {
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(period, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = weight)
        Text(monthly, modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = weight)
        Text(total, modifier = Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall, fontWeight = weight)
    }
}
