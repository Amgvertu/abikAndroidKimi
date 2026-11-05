package info.prorabka.abik.android.presentation.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.prefs.GuideProgressStore
import info.prorabka.abik.android.domain.repository.GuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repository: GuideRepository,
    private val progressStore: GuideProgressStore,
) : ViewModel() {
    data class UiState(
        val loaded: Boolean = false,
        val completed: Set<String> = emptySet(),
        val forcedKey: String? = null,
    )
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init { load() }

    fun load() {
        // Сначала локальный прогресс: справка не зависит от доступности сервера.
        _ui.update { it.copy(loaded = true, completed = it.completed + progressStore.completed()) }
        viewModelScope.launch {
            val result = repository.completed()
            if (result is ApiResult.Success) {
                _ui.update { it.copy(completed = it.completed + result.data.toSet()) }
            }
        }
    }

    fun complete(key: String) {
        val parts = key.split(':', limit = 2)
        if (parts.size != 2) return
        progressStore.complete(key)
        _ui.update { it.copy(completed = it.completed + key, forcedKey = null) }
        viewModelScope.launch { repository.complete(parts[0], parts[1]) }
    }

    /** Закрытие и «Пропустить» тоже означают, что автоматический показ завершён. */
    fun skip(key: String) = complete(key)

    /** Ручной вызов из меню всегда показывает справку текущего экрана заново. */
    fun repeat(key: String) = _ui.update { it.copy(forcedKey = key) }
}
