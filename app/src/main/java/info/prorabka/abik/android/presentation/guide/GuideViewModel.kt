package info.prorabka.abik.android.presentation.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repository: GuideRepository,
) : ViewModel() {
    data class UiState(
        val loaded: Boolean = false,
        val completed: Set<String> = emptySet(),
        val skipped: Set<String> = emptySet(),
        val forcedKey: String? = null,
    )
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init { load() }

    fun load() {
        viewModelScope.launch {
            val result = repository.completed()
            _ui.update {
                it.copy(
                    loaded = true,
                    completed = (result as? ApiResult.Success)?.data?.toSet() ?: it.completed,
                )
            }
        }
    }

    fun complete(key: String) {
        val parts = key.split(':', limit = 2)
        if (parts.size != 2) return
        _ui.update { it.copy(completed = it.completed + key, forcedKey = null) }
        viewModelScope.launch { repository.complete(parts[0], parts[1]) }
    }

    fun skip(key: String) {
        _ui.update { it.copy(skipped = it.skipped + key, forcedKey = null) }
    }

    fun repeat(key: String) = _ui.update { it.copy(forcedKey = key) }
}
