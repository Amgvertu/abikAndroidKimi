package info.prorabka.abik.android.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.util.PhoneUtils
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.TeamCreateData
import info.prorabka.abik.android.domain.model.TeamDetail
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.FileRepository
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
    private val fileRepository: FileRepository,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    data class UiState(
        val loading: Boolean = true,
        val noTeam: Boolean = false,
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
                if (team != null) {
                    load(team.id)
                } else {
                    _ui.update {
                        it.copy(loading = false, noTeam = true, detail = null, members = emptyList())
                    }
                }
            }
        }
    }

    fun load(teamId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, noTeam = false, error = null) }
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

    /** Долгий тап на логотип: загрузить файл и обновить логотип команды. */
    fun changeLogo(uri: android.net.Uri) {
        val teamId = currentTeamId() ?: return
        val detail = _ui.value.detail ?: return
        viewModelScope.launch {
            when (val uploaded = fileRepository.upload(uri)) {
                is ApiResult.Success -> {
                    val result = teamRepository.update(
                        teamId,
                        TeamCreateData(
                            name = detail.name,
                            cityId = detail.cityId ?: 0L,
                            homeRegionId = detail.homeRegionId ?: 0L,
                            logoUrl = uploaded.data,
                            description = detail.description,
                            season = detail.season,
                        ),
                    )
                    when (result) {
                        is ApiResult.Success -> {
                            _ui.update { it.copy(detail = result.data, info = "Логотип обновлён") }
                            teamManager.refresh()
                        }
                        is ApiResult.HttpError -> _ui.update {
                            it.copy(error = result.message)
                        }
                        is ApiResult.NetworkError -> _ui.update {
                            it.copy(error = "Нет соединения с сервером")
                        }
                    }
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = uploaded.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

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
