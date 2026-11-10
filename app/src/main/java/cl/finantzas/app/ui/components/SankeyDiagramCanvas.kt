package cl.finantzas.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import kotlin.math.roundToInt

internal data class SankeyCategoryFlow(
    val category: String,
    val totalClp: Long,
    val essentialClp: Long,
    val antExpenseClp: Long
)

private val MintIncomeColor = Color(0xFF34D399)
private val SkyCenterNodeColor = Color(0xFF7DD3FC)
private val CoralAntNodeColor = Color(0xFFF46036)
private val SlateBlueRibbonColor = Color(0xFF64748B)
private val EmeraldSavingsColor = Color(0xFF1B9E77)

/** Draws monthly spending flows with Compose Canvas inside the Dark Petrol "Flujo de Dinero" card; all amounts in CLP without decimals. */
@Composable
internal fun SankeyDiagramCanvas(
    categories: List<SankeyCategoryFlow>,
    modifier: Modifier = Modifier
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val positiveCategories = categories.filter { it.totalClp > 0L }
    val total = positiveCategories.sumOf { it.totalClp }
    val essentialTotal = positiveCategories.sumOf { it.essentialClp }
    val antExpenseTotal = positiveCategories.sumOf { it.antExpenseClp }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Flujo de Dinero",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ClpCurrencyFormatter.formatClp(total)} CLP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MintIncomeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingresos (${ClpCurrencyFormatter.formatClp(total)})",
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Flujo Central",
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Gastos / Ahorros",
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                drawThreeStageSankeyFlows(
                    categories = positiveCategories,
                    total = total,
                    antExpenseTotal = antExpenseTotal
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SankeyLegendRow(
                    color = CoralAntNodeColor,
                    label = "Gastos / Fuga Hormiga",
                    amount = antExpenseTotal,
                    total = total,
                    textColor = textColor
                )
                SankeyLegendRow(
                    color = EmeraldSavingsColor,
                    label = "Ahorros / Gasto Esencial",
                    amount = essentialTotal,
                    total = total,
                    textColor = textColor
                )
                positiveCategories.forEach { category ->
                    SankeyLegendRow(
                        color = MintIncomeColor,
                        label = category.category.toExpenseCategoryLabel(),
                        amount = category.totalClp,
                        total = total,
                        textColor = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SankeyLegendRow(
    color: Color,
    label: String,
    amount: Long,
    total: Long,
    textColor: Color
) {
    val percentage = if (total > 0L) {
        ((amount.toDouble() / total.toDouble()) * 100.0).roundToInt()
    } else {
        0
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "${ClpCurrencyFormatter.formatClp(amount)} CLP ($percentage%)",
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun DrawScope.drawThreeStageSankeyFlows(
    categories: List<SankeyCategoryFlow>,
    total: Long,
    antExpenseTotal: Long
) {
    if (total <= 0L || size.width <= 0f || size.height <= 0f) return

    val topPadding = 8.dp.toPx()
    val plotHeight = (size.height - topPadding * 2f).coerceAtLeast(1f)
    val barWidth = 8.dp.toPx()

    val leftX = 4.dp.toPx()
    val midX = size.width * 0.48f
    val rightX = size.width - barWidth - 4.dp.toPx()

    val leftGap = if (categories.size > 1) 10.dp.toPx() else 0f
    val totalLeftGaps = leftGap * (categories.size - 1).coerceAtLeast(0)
    val usableLeftHeight = (plotHeight - totalLeftGaps).coerceAtLeast(1f)

    val midHeight = plotHeight * 0.78f
    val midTop = topPadding + (plotHeight - midHeight) / 2f

    var leftCursor = topPadding
    var midInCursor = midTop

    categories.forEachIndexed { index, category ->
        val fraction = safeFraction(category.totalClp, total)
        val nodeHeight = (usableLeftHeight * fraction).coerceAtLeast(6.dp.toPx())
        val targetMidHeight = (midHeight * fraction).coerceAtLeast(4.dp.toPx())

        val ribbonColor = if (index % 2 == 0) {
            SkyCenterNodeColor.copy(alpha = 0.38f)
        } else {
            EmeraldSavingsColor.copy(alpha = 0.45f)
        }

        drawSankeyRibbon(
            startX = leftX + barWidth,
            endX = midX,
            startTop = leftCursor,
            startBottom = leftCursor + nodeHeight,
            endTop = midInCursor,
            endBottom = midInCursor + targetMidHeight,
            color = ribbonColor
        )

        drawRoundRect(
            color = MintIncomeColor,
            topLeft = Offset(leftX, leftCursor),
            size = Size(barWidth, nodeHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Fill
        )

        leftCursor += nodeHeight + leftGap
        midInCursor += targetMidHeight
    }

    val rightGap = 16.dp.toPx()
    val usableRightHeight = (plotHeight - rightGap).coerceAtLeast(1f)

    val antFraction = safeFraction(antExpenseTotal, total).coerceIn(0.15f, 0.85f)
    val essentialFraction = (1f - antFraction).coerceIn(0.15f, 0.85f)

    val topOutHeight = usableRightHeight * antFraction
    val bottomOutHeight = usableRightHeight * essentialFraction

    val midAntHeight = midHeight * antFraction
    val midEssentialHeight = midHeight - midAntHeight

    drawSankeyRibbon(
        startX = midX + barWidth,
        endX = rightX,
        startTop = midTop,
        startBottom = midTop + midAntHeight,
        endTop = topPadding,
        endBottom = topPadding + topOutHeight,
        color = SlateBlueRibbonColor.copy(alpha = 0.45f)
    )
    drawRoundRect(
        color = CoralAntNodeColor,
        topLeft = Offset(rightX, topPadding),
        size = Size(barWidth, topOutHeight),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Fill
    )

    val bottomOutTop = topPadding + topOutHeight + rightGap
    drawSankeyRibbon(
        startX = midX + barWidth,
        endX = rightX,
        startTop = midTop + midAntHeight,
        startBottom = midTop + midAntHeight + midEssentialHeight,
        endTop = bottomOutTop,
        endBottom = bottomOutTop + bottomOutHeight,
        color = EmeraldSavingsColor.copy(alpha = 0.55f)
    )
    drawRoundRect(
        color = EmeraldSavingsColor,
        topLeft = Offset(rightX, bottomOutTop),
        size = Size(barWidth, bottomOutHeight),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Fill
    )

    drawRoundRect(
        color = SkyCenterNodeColor,
        topLeft = Offset(midX, midTop),
        size = Size(barWidth, midHeight),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Fill
    )
}

private fun DrawScope.drawSankeyRibbon(
    startX: Float,
    endX: Float,
    startTop: Float,
    startBottom: Float,
    endTop: Float,
    endBottom: Float,
    color: Color
) {
    val controlOffset = (endX - startX) * 0.48f
    val path = Path().apply {
        moveTo(startX, startTop)
        cubicTo(startX + controlOffset, startTop, endX - controlOffset, endTop, endX, endTop)
        lineTo(endX, endBottom)
        cubicTo(endX - controlOffset, endBottom, startX + controlOffset, startBottom, startX, startBottom)
        close()
    }
    drawPath(path, color)
}

private fun safeFraction(amount: Long, total: Long): Float =
    if (total > 0L) (amount.coerceAtLeast(0L).toDouble() / total.toDouble()).toFloat() else 0f

internal fun String.toExpenseCategoryLabel(): String = when (uppercase()) {
    "HOUSING_AND_UTILITIES" -> "Vivienda y servicios"
    "FOOD_AND_GROCERIES" -> "Alimentación y supermercado"
    "TRANSPORT_AND_MOBILITY" -> "Transporte y movilidad"
    "MICRO_PURCHASES_AND_SNACKS" -> "Microcompras y snacks"
    "ENTERTAINMENT_SUBSCRIPTIONS" -> "Suscripciones de entretenimiento"
    else -> replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
