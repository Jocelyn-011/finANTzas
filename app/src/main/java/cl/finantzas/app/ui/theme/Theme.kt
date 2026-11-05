
package cl.finantzas.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Petrol,
    onPrimary = Color.White,

    secondary = Emerald,
    onSecondary = Color.White,

    tertiary = AlertaHormiga,
    onTertiary = Color.White,

    error = AlertaHormiga,
    onError = Color.White,

    background = FondoClaro,
    onBackground = TextoOscuro,

    surface = SuperficieClara,
    onSurface = TextoOscuro
)

private val DarkColorScheme = darkColorScheme(
    primary = PetrolClaro,
    onPrimary = FondoOscuro,

    secondary = EmeraldClaro,
    onSecondary = FondoOscuro,

    tertiary = AlertaHormigaClara,
    onTertiary = FondoOscuro,

    error = AlertaHormigaClara,

    background = FondoOscuro,
    onBackground = Color.White,

    surface = SuperficieOscura,
    onSurface = Color.White
)

@Composable
fun FinANTzasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
