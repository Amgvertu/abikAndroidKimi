package info.prorabka.abik.android.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.util.PhoneUtils
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.TeamDetail
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val detail: TeamDetail? = null,
        val members: List<TeamMember> = emptyList(),
        val error: String? = null,
        val info: String? = null,
    ) {
        val isManager: Boolean
            get() = detail?.myRole == MemberRole.OWNER ||
                detail?.myRole == MemberRole.MODERATOR ||
                detail?.myRole == MemberRole.ADMIN
        val isOwner: Boolean get() = detail?.myRole == MemberRole.OWNER
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                if (team != null) load(team.id)
            }
        }
    }

    fun load(teamId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(detail = d.data) }
                is ApiResult.HttpError -> _ui.update { it.copy(error = d.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(members = m.data) }
                else -> Unit
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    private fun currentTeamId(): Long? = teamManager.selected.value?.id

    fun invite(phoneRaw: String) {
        val teamId = currentTeamId() ?: return
        val phone = PhoneUtils.normalize(phoneRaw)
        if (phone == null) {
            _ui.update { it.copy(error = "Введите корректный номер телефона") }
            return
        }
        viewModelScope.launch {
            when (val r = teamRepository.invite(teamId, phone)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(info = "Приглашение отправлено на $phone")
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun setJerseyNumber(member: TeamMember, number: Int?) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.setJerseyNumber(teamId, member.userId, number)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun toggleModerator(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        val newRole = if (member.role == MemberRole.MODERATOR) MemberRole.MEMBER
        else MemberRole.MODERATOR
        viewModelScope.launch {
            when (val r = teamRepository.changeRole(teamId, member.userId, newRole)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun exclude(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.excludeMember(teamId, member.userId)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun restore(member: TeamMember) {
        val teamId = currentTeamId() ?: return
        viewModelScope.launch {
            when (val r = teamRepository.restoreMember(teamId, member.userId)) {
                is ApiResult.Success -> load(teamId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }
}
