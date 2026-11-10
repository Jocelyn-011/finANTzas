
package cl.finantzas.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.finantzas.app.ui.components.KpiCards
import cl.finantzas.app.ui.components.ExpenseDemoScreen
import cl.finantzas.app.ui.theme.Petrol
import androidx.compose.foundation.layout.RowScope

private data class Pantalla(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

private val pantallas = listOf(
    Pantalla("registro", "Registro", Icons.Outlined.AccountBalanceWallet),
    Pantalla("graficos", "Gráficos", Icons.Outlined.PieChart),
    Pantalla("simulador", "Simulador", Icons.Outlined.Calculate),
    Pantalla("alertas", "Alertas", Icons.Outlined.Notifications),
    Pantalla("nfc", "NFC", Icons.Outlined.Nfc)
)

@Composable
fun FinANTzasNavigation() {
    val navController = rememberNavController()
    val entradaActual = navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual.value?.destination?.route ?: "registro"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            EncabezadoFinANTzas()
        },
        bottomBar = {
            BarraNavegacion(
                rutaActual = rutaActual,
                onNavigate = { ruta ->
                    navController.navigate(ruta) {
                        popUpTo("registro") {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "registro",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            composable("registro") {
                PantallaRegistro()
            }
            composable("graficos") {
                PantallaTemporal("Sankey, Anillos y % de Fuga")
            }
            composable("simulador") {
                PantallaTemporal("Simulador de Ahorro")
            }
            composable("alertas") {
                PantallaTemporal("Alertas y Presupuestos")
            }
            composable("nfc") {
                PantallaTemporal("Billetera NFC")
            }
        }
    }
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
private fun PantallaRegistro() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registro y Motor Hormiga",
            style = MaterialTheme.typography.titleLarge,
            color = Petrol
        )

        ExpenseDemoScreen()
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
