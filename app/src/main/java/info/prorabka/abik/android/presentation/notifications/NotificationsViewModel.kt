package info.prorabka.abik.android.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.repository.NotificationRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val items: List<AppNotification> = emptyList(),
        val pendingInvitations: Map<Long, Invitation> = emptyMap(),
        val invitationSavingId: Long? = null,
        val loading: Boolean = true,
        val error: String? = null,
        val info: String? = null,
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
            val invitations = teamRepository.incomingInvitations()
            if (invitations is ApiResult.Success) {
                _ui.update {
                    it.copy(
                        pendingInvitations = invitations.data
                            .filter { invitation -> invitation.status == "PENDING" }
                            .associateBy(Invitation::teamId),
                    )
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

    fun acceptInvitation(notification: AppNotification, invitation: Invitation) {
        respondToInvitation(notification, invitation, accept = true)
    }

    fun declineInvitation(notification: AppNotification, invitation: Invitation) {
        respondToInvitation(notification, invitation, accept = false)
    }

    private fun respondToInvitation(
        notification: AppNotification,
        invitation: Invitation,
        accept: Boolean,
    ) {
        if (_ui.value.invitationSavingId != null) return
        viewModelScope.launch {
            _ui.update {
                it.copy(invitationSavingId = invitation.id, error = null, info = null)
            }
            val result = if (accept) {
                teamRepository.acceptInvitation(invitation.id)
            } else {
                teamRepository.declineInvitation(invitation.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    if (accept) {
                        teamManager.refresh()
                        teamManager.teams.value
                            .firstOrNull { it.id == invitation.teamId }
                            ?.let(teamManager::select)
                    }
                    _ui.update { state ->
                        state.copy(
                            items = state.items.map {
                                if (it.id == notification.id) it.copy(isRead = true) else it
                            },
                            pendingInvitations = state.pendingInvitations - invitation.teamId,
                            invitationSavingId = null,
                            info = if (accept) {
                                "Вы вступили в команду «${invitation.teamName}»"
                            } else {
                                "Приглашение отклонено"
                            },
                        )
                    }
                    repository.markRead(notification.id)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(invitationSavingId = null, error = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(invitationSavingId = null, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }

    fun markAllRead() {
        val hadUnread = _ui.value.hasUnread
        if (!hadUnread) return
        _ui.update { state ->
            state.copy(items = state.items.map { it.copy(isRead = true) })
        }
        viewModelScope.launch { repository.markAllRead() }
    }
}
