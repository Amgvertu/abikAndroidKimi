package info.prorabka.abik.android.presentation.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.EventDetail
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.VoteAnswer
import info.prorabka.abik.android.domain.repository.EventRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    val eventId: Long = checkNotNull(savedStateHandle["eventId"])

    data class UiState(
        val loading: Boolean = true,
        val event: EventDetail? = null,
        val myRole: MemberRole? = null,
        val acting: Boolean = false,
        val deleted: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    ) {
        val isManager: Boolean
            get() = myRole == MemberRole.OWNER ||
                myRole == MemberRole.ADMIN ||
                myRole == MemberRole.MODERATOR

        val isOwnerOrAdmin: Boolean
            get() = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        load()
    }

    fun load() {
        val teamId = teamManager.selected.value?.id ?: run {
            _ui.update { it.copy(loading = false, error = "Нет выбранной команды") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = it.event == null, error = null) }
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
                else -> Unit
            }
            when (val r = eventRepository.detail(teamId, eventId)) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, event = r.data) }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun vote(answer: VoteAnswer) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(acting = true, error = null) }
            when (val r = eventRepository.vote(teamId, eventId, answer)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(acting = false, info = "Ответ сохранён") }
                    load()
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(acting = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(acting = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun delete(scope: String) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(acting = true, error = null) }
            when (val r = eventRepository.delete(teamId, eventId, scope)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(acting = false, deleted = true)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(acting = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(acting = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }
}
