package cl.finantzas.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.aiep.finantzas.domain.rules.FinancialThresholds
import cl.finantzas.app.data.local.entity.ExpenseEntityClp
import cl.finantzas.app.ui.theme.CoralHormiga
import cl.finantzas.app.ui.theme.DarkPetrolBg
import cl.finantzas.app.ui.theme.DarkPetrolBorder
import cl.finantzas.app.ui.theme.DarkPetrolCard
import cl.finantzas.app.ui.theme.EmeraldAccent
import cl.finantzas.app.ui.theme.FinANTzasBrandLogo
import cl.finantzas.app.ui.theme.FinANTzasThemeState
import cl.finantzas.app.ui.theme.LocalFinANTzasDarkTheme
import cl.finantzas.app.ui.theme.MintBright
import cl.finantzas.app.ui.viewmodel.BudgetViewModel
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel,
    onNavigateToInsights: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val dashboardState by expenseViewModel.uiState.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()
    val budgetCards by budgetViewModel.cards.collectAsState()

    val globalCard = budgetCards.firstOrNull { it.category == "GLOBAL" }
    val globalLimitClp = globalCard?.limit ?: 950_000L
    val globalSpentClp = globalCard?.spent ?: dashboardState.totalMonthlyClp
    val consumedPct = if (globalLimitClp > 0L) {
        ((globalSpentClp.toDouble() / globalLimitClp.toDouble()) * 100.0).roundToInt()
    } else {
        0
    }
    val progressFraction = (consumedPct / 100f).coerceIn(0f, 1f)

    val statusLabel = when {
        consumedPct >= FinancialThresholds.BUDGET_CRITICAL_PERCENTAGE -> "Presupuesto Excedido"
        consumedPct >= FinancialThresholds.BUDGET_WARNING_PERCENTAGE -> "Alerta Preventiva"
        else -> "Controlado"
    }
    val statusColor = when {
        consumedPct >= FinancialThresholds.BUDGET_CRITICAL_PERCENTAGE -> CoralHormiga
        consumedPct >= FinancialThresholds.BUDGET_WARNING_PERCENTAGE -> Color(0xFFF6AA1C)
        else -> MintBright
    }

    val categories = remember(expenses) {
        val grouped = expenses.groupBy { it.category }
            .mapValues { (_, rows) -> rows.sumOf { it.monthlyImpactClp.coerceAtLeast(0L) } }
            .toList()
            .sortedByDescending { it.second }
        grouped.ifEmpty {
            listOf(
                "FOOD_AND_GROCERIES" to 280_000L,
                "HOUSING_AND_UTILITIES" to 320_000L,
                "MICRO_PURCHASES_AND_SNACKS" to 70_000L,
                "ENTERTAINMENT_SUBSCRIPTIONS" to 26_900L
            )
        }
    }

    var selectedQuickPercent by rememberSaveable { mutableIntStateOf(50) }
    val antMonthlyBaseClp = if (dashboardState.antMonthlyClp > 0L) dashboardState.antMonthlyClp else 70_000L
    val quickMonthlySavings = savingsPerMonth(antMonthlyBaseClp, selectedQuickPercent)
    val quickAnnualSavings = quickMonthlySavings * 12L

    val recentMovements = remember(expenses) {
        expenses.sortedByDescending { it.createdAt }.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado Bento: FinANTzasBrandLogo + Saludo + Campana con Badge + Avatar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                FinANTzasBrandLogo()
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "¡Hola, ${FinANTzasThemeState.userDisplayName}!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onNavigateToBudgets) {
                    BadgedBox(
                        badge = {
                            if (dashboardState.activeAlertsCount > 0) {
                                Badge(
                                    containerColor = CoralHormiga,
                                    contentColor = Color.White
                                ) {
                                    Text(dashboardState.activeAlertsCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones de presupuesto",
                            tint = textColor
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                        .border(1.5.dp, MintBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Avatar de usuario",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Tarjeta Superior: Presupuesto Mensual
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBudgets() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Presupuesto Mensual",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusColor.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$statusLabel · $consumedPct%",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${ClpCurrencyFormatter.formatClp(globalLimitClp)} CLP",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )

                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = EmeraldAccent,
                    trackColor = if (isDark) DarkPetrolBg else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Consumido: ${ClpCurrencyFormatter.formatClp(globalSpentClp)} CLP",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "$consumedPct%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintBright
                    )
                }
            }
        }

        // Fila Central de 2 Tarjetas Cuadradas: Categorías (Izquierda) + Alerta Gasto Hormiga (Derecha)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Izquierda: Mini Anillo Categorías
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(172.dp)
                    .clickable { onNavigateToInsights() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val totalCat = categories.sumOf { it.second }.coerceAtLeast(1L)
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(84.dp)) {
                            var startAngle = -90f
                            categories.forEachIndexed { idx, (_, amount) ->
                                val sweep = 360f * (amount.toFloat() / totalCat.toFloat())
                                drawArc(
                                    color = expenseChartColors[idx % expenseChartColors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 18f, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                        Text(
                            text = "${categories.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textColor
                        )
                    }
                    Text(
                        text = "Ver desglose CLP",
                        fontSize = 11.sp,
                        color = MintBright,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Derecha: Alerta Gasto Hormiga
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(172.dp)
                    .clickable { onNavigateToTransactions() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CoralHormiga.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "Alerta Gasto Hormiga",
                                tint = CoralHormiga,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Alerta Gasto Hormiga",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${ClpCurrencyFormatter.formatClp(dashboardState.antMonthlyClp)} CLP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralHormiga
                        )
                        Text(
                            text = "(RN-02 / RN-03)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "${dashboardState.antAlertsCount} alertas detectadas",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.65f)
                    )
                }
            }
        }

        // Tarjeta Simulador Rápido (25%, 50%, 100%)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulador Rápido",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Ir a Análisis →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MintBright,
                        modifier = Modifier.clickable { onNavigateToInsights() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 50, 100).forEach { pct ->
                        val active = selectedQuickPercent == pct
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (active) EmeraldAccent
                                    else if (isDark) DarkPetrolBg else Color(0xFFE2E8F0)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (active) MintBright else borderColor,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedQuickPercent = pct }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$pct%",
                                color = if (active) Color.White else textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ahorro anual proyectado ($selectedQuickPercent%):",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "${ClpCurrencyFormatter.formatClp(quickAnnualSavings)} CLP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintBright
                    )
                }
            }
        }

        // Tarjeta Inferior: Transacciones Recientes (últimos 3 movimientos)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transacciones Recientes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Ver todas",
                        fontSize = 12.sp,
                        color = MintBright,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToTransactions() }
                    )
                }

                if (recentMovements.isEmpty()) {
                    RecentMovementRowItem(
                        title = "Café Grano Centro",
                        dateText = "Hoy · Micro-compras",
                        amountClp = 3_500L,
                        isAnt = true,
                        textColor = textColor
                    )
                    RecentMovementRowItem(
                        title = "Supermercado Vecinal",
                        dateText = "Ayer · Alimentación",
                        amountClp = 42_000L,
                        isAnt = false,
                        textColor = textColor
                    )
                    RecentMovementRowItem(
                        title = "Suscripción Streaming",
                        dateText = "Mensual · Entretenimiento",
                        amountClp = 12_990L,
                        isAnt = true,
                        textColor = textColor
                    )
                } else {
                    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-CL")) }
                    recentMovements.forEach { exp: ExpenseEntityClp ->
                        RecentMovementRowItem(
                            title = exp.concept,
                            dateText = "${dateFormat.format(Date(exp.createdAt))} · ${exp.category.toExpenseCategoryLabel()}",
                            amountClp = exp.monthlyImpactClp,
                            isAnt = exp.isAntExpense,
                            textColor = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMovementRowItem(
    title: String,
    dateText: String,
    amountClp: Long,
    isAnt: Boolean,
    textColor: Color
) {
    val badgeBg = if (isAnt) CoralHormiga.copy(alpha = 0.2f) else EmeraldAccent.copy(alpha = 0.2f)
    val badgeColor = if (isAnt) CoralHormiga else MintBright

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$",
                    color = badgeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.65f)
                )
            }
        }
        Text(
            text = "-${ClpCurrencyFormatter.formatClp(amountClp)} CLP",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isAnt) CoralHormiga else textColor
        )
    }
}
