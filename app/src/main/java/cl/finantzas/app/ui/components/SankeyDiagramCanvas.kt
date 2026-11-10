package cl.finantzas.app.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import kotlin.math.roundToInt

internal data class SankeyCategoryFlow(
    val category: String,
    val totalClp: Long,
    val essentialClp: Long,
    val antExpenseClp: Long
)

private val essentialColor = Color(0xFF18845B)
private val antExpenseColor = Color(0xFFE45773)

/** Draws monthly spending flows with Compose Canvas; all amounts are CLP. */
@Composable
internal fun SankeyDiagramCanvas(
    categories: List<SankeyCategoryFlow>,
    modifier: Modifier = Modifier
) {
    val positiveCategories = categories.filter { it.totalClp > 0L }
    val total = positiveCategories.sumOf { it.totalClp }
    val essentialTotal = positiveCategories.sumOf { it.essentialClp }
    val antExpenseTotal = positiveCategories.sumOf { it.antExpenseClp }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Total mensual: ${ClpCurrencyFormatter.formatClp(total)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categorías", style = MaterialTheme.typography.labelSmall)
            Text("Tipo de gasto", style = MaterialTheme.typography.labelSmall)
        }

        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            drawSankeyFlows(positiveCategories, total, essentialTotal, antExpenseTotal)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            positiveCategories.forEachIndexed { index, category ->
                SankeyLegendRow(
                    color = expenseChartColors[index % expenseChartColors.size],
                    label = category.category.toExpenseCategoryLabel(),
                    amount = category.totalClp,
                    total = total
                )
            }
            SankeyLegendRow(
                color = essentialColor,
                label = "Gasto esencial",
                amount = essentialTotal,
                total = total
            )
            SankeyLegendRow(
                color = antExpenseColor,
                label = "Gasto hormiga",
                amount = antExpenseTotal,
                total = total
            )
        }
    }
}

@Composable
private fun SankeyLegendRow(color: Color, label: String, amount: Long, total: Long) {
    val percentage = if (total > 0L) {
        ((amount.toDouble() / total.toDouble()) * 100.0).roundToInt()
    } else {
        0
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            "$label: ${ClpCurrencyFormatter.formatClp(amount)} ($percentage%)",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun DrawScope.drawSankeyFlows(
    categories: List<SankeyCategoryFlow>,
    total: Long,
    essentialTotal: Long,
    antExpenseTotal: Long
) {
    if (total <= 0L || size.width <= 0f || size.height <= 0f) return

    val top = 8.dp.toPx()
    val bottom = size.height - top
    val plotHeight = (bottom - top).coerceAtLeast(1f)
    val categoryGap = if (categories.size > 1) 8.dp.toPx() else 0f
    val categoryGaps = categoryGap * (categories.size - 1).coerceAtLeast(0)
    val hasEssential = essentialTotal > 0L
    val hasAntExpense = antExpenseTotal > 0L
    val typeGap = if (hasEssential && hasAntExpense) 12.dp.toPx() else 0f
    val reservedGap = maxOf(categoryGaps, typeGap)
    val flowHeight = (plotHeight - reservedGap).coerceAtLeast(1f)
    val categoryTop = top + (reservedGap - categoryGaps) / 2f
    val typeTop = top + (reservedGap - typeGap) / 2f

    val categoryX = size.width * 0.06f
    val categoryWidth = size.width * 0.19f
    val destinationX = size.width * 0.75f
    val destinationWidth = size.width * 0.19f

    var categoryCursor = categoryTop
    val essentialHeight = flowHeight * safeFraction(essentialTotal, total)
    val antExpenseHeight = flowHeight * safeFraction(antExpenseTotal, total)
    var essentialCursor = typeTop
    var antCursor = typeTop + essentialHeight + typeGap

    categories.forEachIndexed { index, category ->
        val categoryHeight = flowHeight * safeFraction(category.totalClp, total)
        val nodeTop = categoryCursor
        val nodeBottom = nodeTop + categoryHeight
        val color = expenseChartColors[index % expenseChartColors.size]

        var categoryFlowCursor = nodeTop
        val categoryEssentialHeight = if (category.totalClp > 0L) {
            categoryHeight * safeFraction(category.essentialClp, category.totalClp)
        } else 0f
        val categoryAntHeight = (categoryHeight - categoryEssentialHeight).coerceAtLeast(0f)

        if (category.essentialClp > 0L && categoryEssentialHeight > 0f) {
            val targetHeight = flowHeight * safeFraction(category.essentialClp, total)
            drawSankeyRibbon(
                startX = categoryX + categoryWidth,
                endX = destinationX,
                startTop = categoryFlowCursor,
                startBottom = categoryFlowCursor + categoryEssentialHeight,
                endTop = essentialCursor,
                endBottom = essentialCursor + targetHeight,
                color = essentialColor.copy(alpha = 0.54f)
            )
            categoryFlowCursor += categoryEssentialHeight
            essentialCursor += targetHeight
        }

        if (category.antExpenseClp > 0L && categoryAntHeight > 0f) {
            val targetHeight = flowHeight * safeFraction(category.antExpenseClp, total)
            drawSankeyRibbon(
                startX = categoryX + categoryWidth,
                endX = destinationX,
                startTop = categoryFlowCursor,
                startBottom = categoryFlowCursor + categoryAntHeight,
                endTop = antCursor,
                endBottom = antCursor + targetHeight,
                color = antExpenseColor.copy(alpha = 0.58f)
            )
            antCursor += targetHeight
        }

        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(categoryX, nodeTop),
            size = androidx.compose.ui.geometry.Size(categoryWidth, categoryHeight.coerceAtLeast(1f)),
            cornerRadius = CornerRadius(5.dp.toPx()),
            style = Fill
        )
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(categoryX, nodeTop),
            size = androidx.compose.ui.geometry.Size(categoryWidth, categoryHeight.coerceAtLeast(1f)),
            cornerRadius = CornerRadius(5.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        categoryCursor = nodeBottom + categoryGap
    }

    if (essentialHeight > 0f) {
        drawRoundRect(
            color = essentialColor,
            topLeft = androidx.compose.ui.geometry.Offset(destinationX, typeTop),
            size = androidx.compose.ui.geometry.Size(destinationWidth, essentialHeight.coerceAtLeast(1f)),
            cornerRadius = CornerRadius(7.dp.toPx()),
            style = Fill
        )
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(destinationX, typeTop),
            size = androidx.compose.ui.geometry.Size(destinationWidth, essentialHeight.coerceAtLeast(1f)),
            cornerRadius = CornerRadius(7.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
    if (antExpenseHeight > 0f) {
        val antTop = typeTop + essentialHeight + typeGap
        drawRoundRect(
            color = antExpenseColor,
            topLeft = androidx.compose.ui.geometry.Offset(destinationX, antTop),
            size = androidx.compose.ui.geometry.Size(destinationWidth, antExpenseHeight.coerceAtLeast(1f)),
            cornerRadius = CornerRadius(7.dp.toPx()),
            style = Fill
        )
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(destinationX, antTop),
            size = androidx.compose.ui.geometry.Size(destinationWidth, antExpenseHeight.coerceAtLeast(1f)),
            cornerRadius = CornerRadius(7.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
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
    "MICRO_PURCHASES_AND_SNACKS" -> "Microcompras y snacks"
    "FOOD_AND_GROCERIES" -> "Alimentación y supermercado"
    "ENTERTAINMENT_SUBSCRIPTIONS" -> "Suscripciones de entretenimiento"
    else -> replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
