package cl.finantzas.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.finantzas.app.data.local.database.FinantzasDatabase
import cl.finantzas.app.data.local.database.SecureDatabaseProvider
import cl.finantzas.app.data.repository.BudgetRepository
import cl.finantzas.app.data.repository.RoomExpenseRepository
import cl.finantzas.app.ui.components.BudgetScreen
import cl.finantzas.app.ui.components.ExpenseChartsScreen
import cl.finantzas.app.ui.components.ExpenseDemoScreen
import cl.finantzas.app.ui.components.HomeScreen
import cl.finantzas.app.ui.components.KpiCards
import cl.finantzas.app.ui.components.NfcWalletScreen
import cl.finantzas.app.ui.components.SankeyCategoryFlow
import cl.finantzas.app.ui.components.SankeyDiagramCanvas
import cl.finantzas.app.ui.components.SavingsSimulatorScreen
import cl.finantzas.app.ui.components.SettingsScreen
import cl.finantzas.app.ui.components.activeExpenseUser
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
import cl.finantzas.app.workers.BudgetSyncScheduler

private data class Pantalla(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

private const val RUTA_HOME = "home"
private const val RUTA_TRANSACTIONS = "transactions"
private const val RUTA_NFC_PAY = "nfc_pay"
private const val RUTA_INSIGHTS = "insights"
private const val RUTA_SETTINGS = "settings"
private const val RUTA_BUDGETS = "alertas"
private const val MIN_VISIBLE_BADGE_ALERTS = 1

private val pantallasSmartphone = listOf(
    Pantalla(RUTA_HOME, "Home", Icons.Outlined.Home),
    Pantalla(RUTA_TRANSACTIONS, "Transactions", Icons.Outlined.SwapHoriz),
    Pantalla(RUTA_NFC_PAY, "NFC Pay", Icons.Outlined.Nfc),
    Pantalla(RUTA_INSIGHTS, "Insights", Icons.Outlined.BarChart),
    Pantalla(RUTA_SETTINGS, "Settings", Icons.Outlined.Settings)
)

private val itemsRailTablet = listOf(
    Pantalla("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    Pantalla("analytics", "Analytics", Icons.Outlined.BarChart),
    Pantalla("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet),
    Pantalla("goals", "Goals", Icons.Outlined.Flag),
    Pantalla("simulator", "Simulator", Icons.Outlined.Calculate),
    Pantalla("settings", "Settings", Icons.Outlined.Settings)
)

@Composable
fun FinANTzasNavigation() {
    val isDark = LocalFinANTzasDarkTheme.current
    val bgColor = if (isDark) DarkPetrolBg else MaterialTheme.colorScheme.background

    val userId = activeExpenseUser()
    val context = LocalContext.current.applicationContext
    val database = remember(context) { SecureDatabaseProvider.getDatabase(context) }
    val budgetViewModel = rememberBudgetViewModel(database, userId) {
        BudgetSyncScheduler.schedule(context)
    }
    val expenseViewModel = rememberExpenseViewModel(database, userId)
    val dashboardState by expenseViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val entradaActual = navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual.value?.destination?.route ?: RUTA_HOME

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (maxWidth < 600.dp) {
            SmartphoneScaffold(
                navController = navController,
                rutaActual = rutaActual,
                expenseViewModel = expenseViewModel,
                budgetViewModel = budgetViewModel
            )
        } else {
            TabletDashboardLayout(
                activeAlertsCount = dashboardState.activeAlertsCount,
                expenseViewModel = expenseViewModel,
                budgetViewModel = budgetViewModel
            )
        }
    }
}

@Composable
private fun SmartphoneScaffold(
    navController: NavHostController,
    rutaActual: String,
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val bgColor = if (isDark) DarkPetrolBg else MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor,
        bottomBar = {
            BarraNavegacion(
                rutaActual = rutaActual,
                onNavigate = { ruta -> navigateSingleTop(navController, ruta) }
            )
        }
    ) { innerPadding ->
        FinANTzasNavHost(
            navController = navController,
            expenseViewModel = expenseViewModel,
            budgetViewModel = budgetViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TabletDashboardLayout(
    activeAlertsCount: Int,
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val bgColor = if (isDark) DarkPetrolBg else MaterialTheme.colorScheme.background
    val cardBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) DarkPetrolBorder else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    var selectedRailRoute by remember { mutableStateOf("dashboard") }
    val dashboardState by expenseViewModel.uiState.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()

    val sankeyCategories = remember(expenses) {
        val grouped = expenses.groupBy { it.category }
            .map { (category, rows) ->
                SankeyCategoryFlow(
                    category = category,
                    totalClp = rows.sumOf { it.monthlyImpactClp.coerceAtLeast(0L) },
                    essentialClp = rows.filterNot { it.isAntExpense }
                        .sumOf { it.monthlyImpactClp.coerceAtLeast(0L) },
                    antExpenseClp = rows.filter { it.isAntExpense }
                        .sumOf { it.monthlyImpactClp.coerceAtLeast(0L) }
                )
            }
            .sortedByDescending { it.totalClp }
        grouped.ifEmpty {
            listOf(
                SankeyCategoryFlow("FOOD_AND_GROCERIES", 420_000L, 350_000L, 70_000L),
                SankeyCategoryFlow("MICRO_PURCHASES_AND_SNACKS", 135_900L, 40_000L, 95_900L),
                SankeyCategoryFlow("ENTERTAINMENT_SUBSCRIPTIONS", 85_000L, 60_000L, 25_000L)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        NavigationRail(
            modifier = Modifier
                .width(210.dp)
                .fillMaxHeight(),
            containerColor = cardBg,
            contentColor = textColor,
            header = {
                FinANTzasBrandLogo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsRailTablet.forEach { item ->
                    val selected = selectedRailRoute == item.ruta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) EmeraldAccent else Color.Transparent)
                            .clickable { selectedRailRoute = item.ruta }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = item.icono,
                            contentDescription = item.titulo,
                            tint = if (selected) Color.White else textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = item.titulo,
                            color = if (selected) Color.White else textColor.copy(alpha = 0.75f),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¡Hola, ${FinANTzasThemeState.userDisplayName}!",
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NotificationBellButton(
                        activeAlertsCount = activeAlertsCount,
                        tint = textColor,
                        onClick = { selectedRailRoute = "goals" }
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Perfil de usuario",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            when (selectedRailRoute) {
                "analytics" -> ExpenseChartsScreen(expenseViewModel, budgetViewModel)
                "wallet" -> NfcWalletScreen()
                "goals" -> BudgetScreen(budgetViewModel)
                "simulator" -> SavingsSimulatorScreen(expenseViewModel)
                "settings" -> SettingsScreen(budgetViewModel = budgetViewModel)
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1.35f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            KpiCards(uiState = dashboardState)
                            SankeyDiagramCanvas(categories = sankeyCategories)
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    ExpenseChartsScreen(expenseViewModel)
                                }
                            }

                            SavingsSimulatorScreen(expenseViewModel, embedded = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationBellButton(
    activeAlertsCount: Int,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (activeAlertsCount >= MIN_VISIBLE_BADGE_ALERTS) {
                    Badge(containerColor = CoralHormiga, contentColor = Color.White) {
                        Text(text = activeAlertsCount.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificaciones de alertas activas",
                tint = tint
            )
        }
    }
}

private fun navigateSingleTop(navController: NavHostController, ruta: String) {
    navController.navigate(ruta) {
        popUpTo(RUTA_HOME) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun rememberBudgetViewModel(
    database: FinantzasDatabase,
    userId: String,
    scheduleSync: () -> Unit
): BudgetViewModel = viewModel(
    key = "budgets-$userId",
    factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(
                BudgetRepository(database.budgetLimitDao()),
                RoomExpenseRepository(database.expenseDao()),
                userId,
                scheduleSync
            ) as T
        }
    }
)

@Composable
private fun rememberExpenseViewModel(
    database: FinantzasDatabase,
    userId: String
): ExpenseViewModel = viewModel(
    key = "expenses-$userId",
    factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(
                repository = RoomExpenseRepository(database.expenseDao()),
                userId = userId,
                budgetLimitsFlow = BudgetRepository(database.budgetLimitDao()).observe(userId)
            ) as T
        }
    }
)

@Composable
private fun FinANTzasNavHost(
    navController: NavHostController,
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = RUTA_HOME,
        modifier = modifier
    ) {
        composable(RUTA_HOME) {
            HomeScreen(
                expenseViewModel = expenseViewModel,
                budgetViewModel = budgetViewModel,
                onNavigateToInsights = { navigateSingleTop(navController, RUTA_INSIGHTS) },
                onNavigateToBudgets = { navigateSingleTop(navController, RUTA_BUDGETS) },
                onNavigateToTransactions = { navigateSingleTop(navController, RUTA_TRANSACTIONS) }
            )
        }
        composable(RUTA_TRANSACTIONS) {
            PantallaRegistro(expenseViewModel)
        }
        composable(RUTA_NFC_PAY) {
            NfcWalletScreen()
        }
        composable(RUTA_INSIGHTS) {
            ExpenseChartsScreen(
                viewModel = expenseViewModel,
                budgetViewModel = budgetViewModel
            )
        }
        composable(RUTA_SETTINGS) {
            SettingsScreen(budgetViewModel = budgetViewModel)
        }
        composable(RUTA_BUDGETS) {
            BudgetScreen(budgetViewModel)
        }
    }
}

@Composable
private fun BarraNavegacion(
    rutaActual: String,
    onNavigate: (String) -> Unit
) {
    val isDark = LocalFinANTzasDarkTheme.current
    val navBg = if (isDark) DarkPetrolCard else MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    NavigationBar(
        containerColor = navBg,
        contentColor = textColor
    ) {
        pantallasSmartphone.forEach { pantalla ->
            val seleccionada = rutaActual == pantalla.ruta

            ItemNavegacion(
                pantalla = pantalla,
                seleccionada = seleccionada,
                textColor = textColor,
                onClick = {
                    if (!seleccionada) {
                        onNavigate(pantalla.ruta)
                    }
                }
            )
        }
    }
}

@Composable
private fun RowScope.ItemNavegacion(
    pantalla: Pantalla,
    seleccionada: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = seleccionada,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = pantalla.icono,
                contentDescription = pantalla.titulo
            )
        },
        label = {
            Text(
                text = pantalla.titulo,
                maxLines = 1,
                fontSize = 11.sp,
                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal
            )
        },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MintBright,
            selectedTextColor = MintBright,
            indicatorColor = EmeraldAccent.copy(alpha = 0.25f),
            unselectedIconColor = textColor.copy(alpha = 0.65f),
            unselectedTextColor = textColor.copy(alpha = 0.65f)
        )
    )
}

@Composable
private fun PantallaRegistro(expenseViewModel: ExpenseViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpenseDemoScreen(
            viewModel = expenseViewModel,
            modifier = Modifier.weight(1f)
        )
    }
}
