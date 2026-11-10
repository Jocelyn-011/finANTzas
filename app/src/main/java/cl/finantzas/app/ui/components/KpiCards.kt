package cl.finantzas.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.ui.viewmodel.ExpenseDashboardUiState

private val DarkPetrolCardColor = Color(0xFF123540)
private val DarkPetrolBorderColor = Color(0xFF1E4E5D)
private val MintSparkColor = Color(0xFF34D399)
private val CoralSparkColor = Color(0xFFF46036)

@Composable
fun KpiCards(
    uiState: ExpenseDashboardUiState,
    modifier: Modifier = Modifier
) {
    val budgetLimit = uiState.budgetEvaluations.firstOrNull { it.category == "GLOBAL" }?.budgetLimitClp
        ?: 850_000L
    val balanceClp = (budgetLimit - uiState.totalMonthlyClp).coerceAtLeast(0L)
    KpiCards(
        gastoMensual = if (uiState.totalMonthlyClp > 0L) uiState.totalMonthlyClp else 355_000L,
        proyeccionAnual = if (uiState.projectedAnnualClp > 0L) uiState.projectedAnnualClp else 135_900L,
        gastoHormiga = if (uiState.antMonthlyClp > 0L) uiState.antMonthlyClp else 28_250L,
        ahorroPotencial = balanceClp.takeIf { uiState.totalMonthlyClp > 0L } ?: 850_000L,
        modifier = modifier
    )
}

@Composable
fun KpiCards(
    gastoMensual: Long,
    proyeccionAnual: Long,
    gastoHormiga: Long,
    ahorroPotencial: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BalancePrimaryKpiCard(
                titulo = "Balance / Presupuesto",
                montoClp = ahorroPotencial,
                modifier = Modifier.weight(1f)
            )

            SparklineKpiCard(
                titulo = "Gasto Mensual",
                subtitulo = "Flujo mensual CLP",
                montoClp = gastoMensual,
                indicadorTexto = "▲ +18%",
                indicadorColor = MintSparkColor,
                points = listOf(0.75f, 0.55f, 0.65f, 0.35f, 0.45f, 0.18f),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SparklineKpiCard(
                titulo = "Proyección Anual",
                subtitulo = "Proyección 12 meses",
                montoClp = proyeccionAnual,
                indicadorTexto = "▲ +18%",
                indicadorColor = MintSparkColor,
                points = listOf(0.80f, 0.68f, 0.58f, 0.62f, 0.38f, 0.22f),
                modifier = Modifier.weight(1f)
            )

            SparklineKpiCard(
                titulo = "Fuga Gasto Hormiga",
                subtitulo = "Microcompras / mes",
                montoClp = gastoHormiga,
                indicadorTexto = "▼ -26%",
                indicadorColor = CoralSparkColor,
                points = listOf(0.25f, 0.42f, 0.35f, 0.65f, 0.52f, 0.78f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BalancePrimaryKpiCard(
    titulo: String,
    montoClp: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkPetrolCardColor),
        border = BorderStroke(1.dp, DarkPetrolBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = ClpCurrencyFormatter.formatClp(montoClp) + " CLP",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SparklineKpiCard(
    titulo: String,
    subtitulo: String,
    montoClp: Long,
    indicadorTexto: String,
    indicadorColor: Color,
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkPetrolCardColor),
        border = BorderStroke(1.dp, DarkPetrolBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = ClpCurrencyFormatter.formatClp(montoClp) + " CLP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitulo,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = indicadorTexto,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = indicadorColor
                )
                MiniSparklineCanvas(
                    points = points,
                    lineColor = indicadorColor
                )
            }
        }
    }
}

@Composable
fun MiniSparklineCanvas(
    points: List<Float> = listOf(0.75f, 0.52f, 0.64f, 0.34f, 0.42f, 0.18f),
    lineColor: Color = MintSparkColor,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 64.dp, height = 24.dp)) {
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1).toFloat()
        val strokePath = Path()
        val glowPath = Path()

        points.forEachIndexed { index, normalizedY ->
            val x = index * stepX
            val y = (normalizedY.coerceIn(0.05f, 0.95f)) * size.height
            if (index == 0) {
                strokePath.moveTo(x, y)
                glowPath.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevY = (points[index - 1].coerceIn(0.05f, 0.95f)) * size.height
                val controlX1 = prevX + (x - prevX) / 2f
                val controlX2 = prevX + (x - prevX) / 2f
                strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                glowPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        glowPath.lineTo(size.width, size.height)
        glowPath.lineTo(0f, size.height)
        glowPath.close()

        drawPath(
            path = glowPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(
            path = strokePath,
            color = lineColor.copy(alpha = 0.35f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
