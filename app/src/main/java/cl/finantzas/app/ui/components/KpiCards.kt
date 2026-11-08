
package cl.finantzas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.aiep.finantzas.domain.rules.ClpCurrencyFormatter
import cl.finantzas.app.ui.theme.AlertaHormiga
import cl.finantzas.app.ui.theme.Emerald
import cl.finantzas.app.ui.theme.Petrol
import cl.finantzas.app.ui.viewmodel.ExpenseDashboardUiState
import java.text.NumberFormat
import java.util.Locale

private fun formatoCLP(monto: Long): String {
    val formato = NumberFormat.getIntegerInstance(
        Locale.forLanguageTag("es-CL")
    )
    return "$" + formato.format(monto)
}

@Composable
fun KpiCards(
    uiState: ExpenseDashboardUiState,
    modifier: Modifier = Modifier
) {
    KpiCards(
        gastoMensual = uiState.totalMonthlyClp,
        proyeccionAnual = uiState.projectedAnnualClp,
        gastoHormiga = uiState.antMonthlyClp,
        ahorroPotencial = uiState.annualSavingsClp,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                titulo = "Gasto mensual total",
                monto = gastoMensual,
                color = Petrol,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                titulo = "Proyección anual",
                monto = proyeccionAnual,
                color = Petrol,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                titulo = "Gasto hormiga / mes",
                monto = gastoHormiga,
                color = AlertaHormiga,
                modifier = Modifier.weight(1f)
            )

            KpiCard(
                titulo = "Ahorro potencial / año",
                monto = ahorroPotencial,
                color = Emerald,
                modifier = Modifier.weight(1f),
                textoFormateado = ClpCurrencyFormatter.formatClp(ahorroPotencial)
            )
        }
    }
}

@Composable
private fun KpiCard(
    titulo: String,
    monto: Long,
    color: Color,
    modifier: Modifier = Modifier,
    textoFormateado: String = formatoCLP(monto)
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = textoFormateado,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
