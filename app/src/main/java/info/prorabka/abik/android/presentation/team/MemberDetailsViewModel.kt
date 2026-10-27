package info.prorabka.abik.android.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.domain.model.MemberDetails
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberDetailsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    data class UiState(
        val loading: Boolean = true,
        val details: MemberDetails? = null,
        val members: List<TeamMember> = emptyList(),
        val myRole: MemberRole? = null,
        val error: String? = null,
        val info: String? = null,
    ) {
        val isManager: Boolean
            get() = myRole == MemberRole.OWNER ||
                myRole == MemberRole.MODERATOR ||
                myRole == MemberRole.ADMIN
        val isOwner: Boolean get() = myRole == MemberRole.OWNER

        /** Тренера назначает только владелец или админ (как на сервере). */
        val canManageTrainer: Boolean
            get() = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var userId: Long = 0L

    fun load(userId: Long) {
        this.userId = userId
        val teamId = teamManager.selected.value?.id
        if (teamId == null) {
            _ui.update { it.copy(loading = false, error = "Команда не выбрана") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
                else -> Unit
            }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(members = m.data) }
                else -> Unit
            }
            when (val r = teamRepository.memberDetails(teamId, userId)) {
                is ApiResult.Success -> _ui.update { it.copy(details = r.data, loading = false) }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun setJerseyNumber(number: Int?) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            when (val r = teamRepository.setJerseyNumber(teamId, userId, number)) {
                is ApiResult.Success -> load(userId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun toggleModerator() {
        val teamId = teamManager.selected.value?.id ?: return
        val current = _ui.value.details ?: return
        val newRole = if (current.role == MemberRole.MODERATOR) MemberRole.MEMBER
        else MemberRole.MODERATOR
        viewModelScope.launch {
            when (val r = teamRepository.changeRole(teamId, userId, newRole)) {
                is ApiResult.Success -> load(userId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun toggleTrainer() {
        val teamId = teamManager.selected.value?.id ?: return
        val current = _ui.value.details ?: return
        viewModelScope.launch {
            when (val r = teamRepository.setTrainer(teamId, userId, !current.isTrainer)) {
                is ApiResult.Success -> load(userId)
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }
}
