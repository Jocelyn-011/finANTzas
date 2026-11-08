package cl.finantzas.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import cl.finantzas.app.ui.components.activeExpenseUser
import cl.finantzas.app.ui.theme.Petrol
import cl.finantzas.app.ui.viewmodel.BudgetViewModel
import cl.finantzas.app.ui.viewmodel.ExpenseViewModel
import cl.finantzas.app.workers.BudgetSyncScheduler

private data class Pantalla(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

private const val RUTA_REGISTRO = "registro"
private const val RUTA_ALERTAS = "alertas"
private const val MIN_VISIBLE_BADGE_ALERTS = 1

private val pantallas = listOf(
    Pantalla(RUTA_REGISTRO, "Registro", Icons.Outlined.AccountBalanceWallet),
    Pantalla("graficos", "Gráficos", Icons.Outlined.PieChart),
    Pantalla("simulador", "Simulador", Icons.Outlined.Calculate),
    Pantalla(RUTA_ALERTAS, "Alertas", Icons.Outlined.Notifications),
    Pantalla("nfc", "NFC", Icons.Outlined.Nfc)
)

@Composable
fun FinANTzasNavigation() {
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
    val rutaActual = entradaActual.value?.destination?.route ?: RUTA_REGISTRO

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FinANTzasTopAppBar(
                activeAlertsCount = dashboardState.activeAlertsCount,
                onNotificationsClick = {
                    if (rutaActual != RUTA_ALERTAS) {
                        navigateSingleTop(navController, RUTA_ALERTAS)
                    }
                }
            )
        },
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
                .padding(16.dp)
        )
    }
}

private fun navigateSingleTop(navController: NavHostController, ruta: String) {
    navController.navigate(ruta) {
        popUpTo(RUTA_REGISTRO) {
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
        startDestination = RUTA_REGISTRO,
        modifier = modifier
    ) {
        composable(RUTA_REGISTRO) {
            PantallaRegistro(expenseViewModel)
        }
        composable("graficos") {
            ExpenseChartsScreen(expenseViewModel)
        }
        composable("simulador") {
            PantallaTemporal("Simulador de Ahorro")
        }
        composable(RUTA_ALERTAS) {
            BudgetScreen(budgetViewModel)
        }
        composable("nfc") {
            PantallaTemporal("Billetera NFC")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinANTzasTopAppBar(
    activeAlertsCount: Int,
    onNotificationsClick: () -> Unit
) {
    TopAppBar(
        title = { EncabezadoFinANTzas() },
        actions = {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (activeAlertsCount >= MIN_VISIBLE_BADGE_ALERTS) {
                            Badge {
                                Text(text = activeAlertsCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones de alertas activas",
                        tint = Petrol
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun EncabezadoFinANTzas() {
    Text(
        text = "finANTzas",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Petrol,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
private fun BarraNavegacion(
    rutaActual: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Petrol
    ) {
        pantallas.forEach { pantalla ->
            val seleccionada = rutaActual == pantalla.ruta

            ItemNavegacion(
                pantalla = pantalla,
                seleccionada = seleccionada,
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
                fontWeight = if (seleccionada) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Petrol,
            indicatorColor = Petrol,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray
        )
    )
}

@Composable
private fun PantallaRegistro(expenseViewModel: ExpenseViewModel) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registro y Motor Hormiga",
            style = MaterialTheme.typography.titleLarge,
            color = Petrol
        )

        ExpenseDemoScreen(viewModel = expenseViewModel)
    }
}

@Composable
private fun PantallaTemporal(titulo: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = Petrol
        )

        Text(
            text = "Pantalla preparada para desarrollo.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
