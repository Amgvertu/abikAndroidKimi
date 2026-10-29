package info.prorabka.abik.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.repository.EventRepository
import info.prorabka.abik.android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Главная: ближайшее событие и последние уведомления выбранной команды.
 * Балансы живут в ShellViewModel — здесь только лента.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val teamManager: CurrentTeamManager,
    private val eventRepository: EventRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val nearestEvent: EventListItem? = null,
        val notifications: List<AppNotification> = emptyList(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        // Перезагружаем ленту при смене команды
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                if (team != null) load(team.id) else _ui.update {
                    it.copy(loading = false, nearestEvent = null, notifications = emptyList())
                }
            }
        }
    }

    fun refresh() {
        teamManager.selected.value?.let { team ->
            viewModelScope.launch { load(team.id) }
        }
    }

    private suspend fun load(teamId: Long) {
        _ui.update { it.copy(loading = true) }
        loadNearestEvent(teamId)
        loadNotifications()
        _ui.update { it.copy(loading = false) }
    }

    private suspend fun loadNearestEvent(teamId: Long) {
        val from = LocalDate.now()
        val to = from.plusMonths(2)
        when (val r = eventRepository.events(teamId, from.toString(), to.toString())) {
            is ApiResult.Success -> {
                val today = from.toString()
                val nearest = r.data
                    .filter { it.dateIso >= today }
                    .sortedWith(compareBy({ it.dateIso }, { it.time }))
                    .firstOrNull()
                _ui.update { it.copy(nearestEvent = nearest) }
            }
            else -> _ui.update { it.copy(nearestEvent = null) }
        }
    }

    private suspend fun loadNotifications() {
        when (val r = notificationRepository.list()) {
            is ApiResult.Success -> _ui.update {
                it.copy(notifications = r.data.sortedByDescending { n -> n.sentAtIso }.take(3))
            }
            else -> _ui.update { it.copy(notifications = emptyList()) }
        }
    }
}
