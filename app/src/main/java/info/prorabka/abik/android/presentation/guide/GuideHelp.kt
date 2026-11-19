package info.prorabka.abik.android.presentation.guide

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Запуск справки предоставляется корневой навигацией с учётом маршрута и роли. */
val LocalGuideLauncher = staticCompositionLocalOf<(String) -> Unit>({ {} })

@Composable
fun GuideHelpButton(screen: String = "") {
    val launch = LocalGuideLauncher.current
    IconButton(onClick = { launch(screen) }) {
        Icon(Icons.Default.HelpOutline, contentDescription = "Справка по текущему экрану")
    }
}
