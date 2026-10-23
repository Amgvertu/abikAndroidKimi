package info.prorabka.abik.android.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Sky = Color(0xFF0EA5E9)
val SkyDark = Color(0xFF38BDF8)
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Danger = Color(0xFFEF4444)

private val DarkColors = darkColorScheme(
    primary = SkyDark,
    onPrimary = Color(0xFF082F49),
    secondary = Color(0xFF7DD3FC),
    background = Color(0xFF060D18),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0D1729),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF16233A),
    onSurfaceVariant = Color(0xFF9FB0C7),
    outline = Color(0xFF263855),
    error = Danger,
)

private val LightColors = lightColorScheme(
    primary = Sky,
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8F1F8),
    onSurfaceVariant = Color(0xFF52677E),
    outline = Color(0xFFD3E2EE),
    error = Danger,
)

@Composable
fun AbikTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
