package cl.finantzas.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object FinANTzasThemeState {
    var isDarkTheme by mutableStateOf(true)
    var userDisplayName by mutableStateOf("Usuario")
}

val LocalFinANTzasDarkTheme = staticCompositionLocalOf { true }

private val DarkPetrolColorScheme = darkColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color.White,

    secondary = MintBright,
    onSecondary = DarkPetrolBg,

    tertiary = CoralHormiga,
    onTertiary = Color.White,

    error = CoralHormiga,
    onError = Color.White,

    background = DarkPetrolBg,
    onBackground = Color.White,

    surface = DarkPetrolCard,
    onSurface = Color.White,

    surfaceVariant = DarkPetrolCard,
    onSurfaceVariant = Color.White,

    outline = DarkPetrolBorder
)

private val LightPetrolColorScheme = lightColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color.White,

    secondary = Petrol,
    onSecondary = Color.White,

    tertiary = CoralHormiga,
    onTertiary = Color.White,

    error = CoralHormiga,
    onError = Color.White,

    background = FondoClaro,
    onBackground = TextoOscuro,

    surface = SuperficieClara,
    onSurface = TextoOscuro,

    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextoOscuro,

    outline = Color(0xFFCBD5E1)
)

@Composable
fun FinANTzasTheme(
    darkTheme: Boolean = FinANTzasThemeState.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPetrolColorScheme else LightPetrolColorScheme
    CompositionLocalProvider(LocalFinANTzasDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun FinANTzasBrandLogo(modifier: Modifier = Modifier) {
    val isDark = LocalFinANTzasDarkTheme.current
    val baseColor = if (isDark) Color.White else TextoOscuro
    val antAccentColor = if (isDark) MintBright else EmeraldAccent

    Box(
        modifier = modifier.padding(top = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "fin",
                color = baseColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "ANT",
                color = antAccentColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Box(contentAlignment = Alignment.TopCenter) {
                Canvas(
                    modifier = Modifier
                        .size(width = 28.dp, height = 18.dp)
                        .offset(x = 2.dp, y = (-12).dp)
                ) {
                    rotate(degrees = -16f) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.55f
                        // Abdomen
                        drawOval(
                            color = baseColor,
                            topLeft = Offset(cx + 3.dp.toPx(), cy - 3.5.dp.toPx()),
                            size = Size(9.dp.toPx(), 6.5.dp.toPx())
                        )
                        // Tórax
                        drawOval(
                            color = baseColor,
                            topLeft = Offset(cx - 2.5.dp.toPx(), cy - 2.dp.toPx()),
                            size = Size(5.5.dp.toPx(), 4.dp.toPx())
                        )
                        // Cabeza
                        drawCircle(
                            color = baseColor,
                            radius = 2.5.dp.toPx(),
                            center = Offset(cx - 5.dp.toPx(), cy - 0.5.dp.toPx())
                        )
                        // Antenas
                        val stroke = 1.2.dp.toPx()
                        drawLine(
                            color = baseColor,
                            start = Offset(cx - 6.5.dp.toPx(), cy - 2.dp.toPx()),
                            end = Offset(cx - 10.dp.toPx(), cy - 6.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = baseColor,
                            start = Offset(cx - 6.dp.toPx(), cy - 2.5.dp.toPx()),
                            end = Offset(cx - 7.5.dp.toPx(), cy - 6.5.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        // Patas
                        drawLine(
                            color = baseColor,
                            start = Offset(cx - 1.dp.toPx(), cy + 1.dp.toPx()),
                            end = Offset(cx - 4.dp.toPx(), cy + 6.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = baseColor,
                            start = Offset(cx + 0.5.dp.toPx(), cy + 1.dp.toPx()),
                            end = Offset(cx + 0.5.dp.toPx(), cy + 6.5.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = baseColor,
                            start = Offset(cx + 2.dp.toPx(), cy + 1.dp.toPx()),
                            end = Offset(cx + 5.5.dp.toPx(), cy + 6.dp.toPx()),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
                Text(
                    text = "zas",
                    color = baseColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
