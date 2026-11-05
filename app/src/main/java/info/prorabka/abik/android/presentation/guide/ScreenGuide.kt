package info.prorabka.abik.android.presentation.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/** Первый показ подсказок на дополнительном экране. */
@Composable
fun ScreenGuide(
    screenKey: String,
    roleKey: String = "MANAGER",
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val key = "$screenKey:$roleKey"
    val visible = ui.forcedKey ?: key.takeIf {
        ui.loaded && it !in ui.completed
    }
    visible?.let { GuideOverlay(it, viewModel) }
}
