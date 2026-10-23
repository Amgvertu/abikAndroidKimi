package info.prorabka.abik.android.presentation.notifications

import androidx.compose.runtime.Composable
import info.prorabka.abik.android.core.ui.components.EmptyState

/**
 * Лента уведомлений. REST-эндпоинты появятся на сервере
 * (backend-api-extension.md, раздел 3) — тогда здесь будет список.
 */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    EmptyState("Лента уведомлений появится после обновления сервера")
}
