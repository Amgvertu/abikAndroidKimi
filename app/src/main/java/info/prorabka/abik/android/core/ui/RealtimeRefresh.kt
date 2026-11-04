package info.prorabka.abik.android.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.RealtimeManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RealtimeViewModel @Inject constructor(
    val manager: RealtimeManager,
) : ViewModel()

/** Обновляет только открытый экран и объединяет серию сигналов в один запрос. */
@OptIn(FlowPreview::class)
@Composable
fun RealtimeRefresh(
    viewModel: RealtimeViewModel = hiltViewModel(),
    onRefresh: () -> Unit,
) {
    val currentRefresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(viewModel) {
        viewModel.manager.start()
        viewModel.manager.revision.drop(1).debounce(400).collectLatest { currentRefresh() }
    }
}
