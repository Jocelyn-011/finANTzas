package cl.finantzas.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.PaymentMethodLeakageResult
import cl.finantzas.app.ui.theme.AlertaHormiga
import cl.finantzas.app.ui.theme.Emerald
import cl.finantzas.app.ui.theme.Petrol
import cl.finantzas.app.ui.viewmodel.ExpenseChartsUiState
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel

private const val FULL_CIRCLE_DEGREES: Float = 360f
private const val START_ANGLE_OFFSET: Float = -90f
private const val ZERO_FLOAT: Float = 0f
private const val DEFAULT_STROKE_WIDTH_DP: Int = 26
private const val HIGH_LEAKAGE_THRESHOLD: Int = 50

/**
 * Modelo de datos inmutable para cada segmento del gráfico de anillo (Donut Chart).
 */
data class DonutSliceUiModel(
    val label: String,
    val amountClp: Long,
    val sweepPercentage: Float,
    val color: Color
)

@Composable
fun ExpenseChartsScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analítica y Distribución de Gastos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Petrol
            )
        }

        item {
            ChartSectionCard(
                title = "Distribución por Categorías",
                subtitle = "Proporción del gasto mensual total según clasificación de consumo"
            ) {
                DonutChartWithLegend(slices = uiState.categorySlices)
            }
        }

        item {
            ChartSectionCard(
                title = "División por Medio de Pago",
                subtitle = "Identifica qué instrumento concentra el mayor volumen de transacciones"
            ) {
                DonutChartWithLegend(slices = uiState.paymentSlices)
            }
        }

        item {
            Text(
                text = "Ranking de Porcentaje de Fuga por Medio de Pago (RN-04)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Petrol,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(uiState.paymentLeakageRanking) { rankingItem ->
            PaymentLeakageRankingCard(item = rankingItem)
        }
    }
}

@Composable
fun DonutChartCanvas(
    slices: List<DonutSliceUiModel>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = DEFAULT_STROKE_WIDTH_DP.dp
) {
    val totalAmount = slices.fold(0L) { acc, slice -> acc + slice.amountClp }

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()

        if (slices.isEmpty() || totalAmount <= 0L) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.5f),
                startAngle = ZERO_FLOAT,
                sweepAngle = FULL_CIRCLE_DEGREES,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            return@Canvas
        }

        var currentStartAngle = START_ANGLE_OFFSET
        slices.forEach { slice ->
            val sweepAngle = (slice.sweepPercentage / 100f) * FULL_CIRCLE_DEGREES
            drawArc(
                color = slice.color,
                startAngle = currentStartAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
            currentStartAngle += sweepAngle
        }
    }
}

@Composable
private fun DonutChartWithLegend(slices: List<DonutSliceUiModel>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChartCanvas(
                slices = slices,
                modifier = Modifier.size(140.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEach { slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${ClpCurrencyFormatter.formatClp(slice.amountClp)} (${slice.sweepPercentage.toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentLeakageRankingCard(
    item: PaymentMethodLeakageResult,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (item.leakagePercentage >= HIGH_LEAKAGE_THRESHOLD) {
        AlertaHormiga
    } else if (item.leakagePercentage > 0) {
        Petrol
    } else {
        Emerald
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Column {
                    Text(
                        text = item.instrument.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Gasto Total: ${ClpCurrencyFormatter.formatClp(item.totalExpenseAmountClp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.leakagePercentage}% de fuga",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
                Text(
                    text = ClpCurrencyFormatter.formatClp(item.antExpenseAmountClp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Petrol
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}