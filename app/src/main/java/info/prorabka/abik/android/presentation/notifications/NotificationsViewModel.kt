package info.prorabka.abik.android.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    data class UiState(
        val items: List<AppNotification> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null,
    ) {
        val hasUnread: Boolean get() = items.any { !it.isRead }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = repository.list()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(items = result.data, loading = false)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Пометить прочитанным при тапе (оптимистично, ошибку молча игнорируем). */
    fun onNotificationClick(notification: AppNotification) {
        if (notification.isRead) return
        _ui.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.id == notification.id) it.copy(isRead = true) else it
                },
            )
        }
        viewModelScope.launch { repository.markRead(notification.id) }
    }

    fun markAllRead() {
        val hadUnread = _ui.value.hasUnread
        if (!hadUnread) return
        _ui.update { state ->
            state.copy(items = state.items.map { it.copy(isRead = true) })
        }
        viewModelScope.launch { repository.markAllRead() }
    }
}
