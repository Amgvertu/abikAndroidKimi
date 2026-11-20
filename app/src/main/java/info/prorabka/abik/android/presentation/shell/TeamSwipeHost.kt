package info.prorabka.abik.android.presentation.shell

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Переключает команду горизонтальным свайпом, не перехватывая события у содержимого.
 * direction: 1 — следующая команда (свайп влево), -1 — предыдущая (вправо).
 */
@Composable
fun TeamSwipeHost(
    enabled: Boolean,
    onSwipe: (direction: Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val thresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val gestureModifier = if (enabled) {
        Modifier.pointerInput(thresholdPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                var distanceX = 0f
                var distanceY = 0f
                var cancelled = false

                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } > 1) cancelled = true
                    if (event.changes.any { it.isConsumed && it.positionChange() != androidx.compose.ui.geometry.Offset.Zero }) {
                        // Горизонтальный список, Pager или drag уже обработали этот жест.
                        cancelled = true
                    }
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) break
                    if (cancelled) continue

                    distanceX += change.position.x - change.previousPosition.x
                    distanceY += change.position.y - change.previousPosition.y
                    if (abs(distanceX) >= thresholdPx && abs(distanceX) > abs(distanceY) * 1.4f) {
                        currentOnSwipe(if (distanceX < 0) 1 else -1)
                        break
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize().then(gestureModifier)) {
        content()
    }
}
