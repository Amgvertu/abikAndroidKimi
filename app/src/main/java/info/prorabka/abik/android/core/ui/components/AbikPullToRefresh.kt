package info.prorabka.abik.android.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Общее обновление свайпом вниз для экранов с серверными данными. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbikPullToRefresh(
    loading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }

    // Даже быстрый ответ показываем короткой анимацией. При сетевой ошибке
    // ViewModel снимает loading, поэтому индикатор не остаётся на экране.
    LaunchedEffect(refreshing, loading) {
        if (refreshing) {
            delay(350)
            if (!loading) refreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}
