package info.prorabka.abik.android.presentation.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeSubgroup
import info.prorabka.abik.android.domain.model.EntryFeeData
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.RosterItem
import info.prorabka.abik.android.domain.model.RosterStatus
import info.prorabka.abik.android.domain.model.Season
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.RosterRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val rosterRepository: RosterRepository,
    private val teamRepository: TeamRepository,
    private val chargeRepository: ChargeRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val noTeam: Boolean = false,
        val seasons: List<Season> = emptyList(),
        val selectedSeason: String? = null,
        val isCurrentSeason: Boolean = false,
        val roster: List<RosterItem> = emptyList(),
        val members: List<TeamMember> = emptyList(),
        val myRole: MemberRole? = null,
        val groups: List<ChargeGroup> = emptyList(),
        val subgroups: List<ChargeSubgroup> = emptyList(),
        val saving: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    ) {
        /** Менять состав могут владелец/админ и только в текущем сезоне. */
        val canManage: Boolean
            get() = isCurrentSeason &&
                (myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN)

        val activeRoster: List<RosterItem>
            get() = roster.filter { it.status == RosterStatus.ACTIVE }
        val removedRoster: List<RosterItem>
            get() = roster.filter { it.status == RosterStatus.REMOVED }

        /** Активные участники команды, которых ещё нет в активном составе. */
        val candidates: List<TeamMember>
            get() {
                val inRoster = activeRoster.map { it.userId }.toSet()
                return members.filter { it.userId !in inRoster }
            }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        val teamId = teamManager.selected.value?.id
        if (teamId == null) {
            _ui.update { it.copy(loading = false, noTeam = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, noTeam = false, error = null) }
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
                else -> Unit
            }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(members = m.data.filter { mm -> mm.status == MemberStatus.ACTIVE })
                }
                else -> Unit
            }
            when (val g = chargeRepository.groups(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(groups = g.data) }
                else -> Unit
            }
            when (val s = rosterRepository.seasons(teamId)) {
                is ApiResult.Success -> {
                    val seasons = s.data
                    val current = _ui.value.selectedSeason
                        ?.takeIf { sel -> seasons.any { it.name == sel } }
                        ?: seasons.firstOrNull { it.isCurrent }?.name
                        ?: seasons.firstOrNull()?.name
                    _ui.update { it.copy(seasons = seasons, selectedSeason = current) }
                    if (current != null) loadRoster(teamId, current)
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = s.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectSeason(season: String) {
        val teamId = teamManager.selected.value?.id ?: return
        _ui.update { it.copy(selectedSeason = season, error = null, info = null) }
        viewModelScope.launch { loadRoster(teamId, season) }
    }

    private suspend fun loadRoster(teamId: Long, season: String) {
        val isCurrent = _ui.value.seasons.firstOrNull { it.name == season }?.isCurrent ?: false
        when (val r = rosterRepository.roster(teamId, season)) {
            is ApiResult.Success -> _ui.update {
                it.copy(roster = r.data, isCurrentSeason = isCurrent)
            }
            is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
            is ApiResult.NetworkError -> _ui.update {
                it.copy(error = "Нет соединения с сервером")
            }
        }
    }

    fun loadSubgroups(groupId: Long) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            when (val s = chargeRepository.subgroups(teamId, groupId)) {
                is ApiResult.Success -> _ui.update { it.copy(subgroups = s.data) }
                else -> Unit
            }
        }
    }

    fun addMember(userId: Long, entryFee: EntryFeeData?) {
        val teamId = teamManager.selected.value?.id ?: return
        val season = _ui.value.selectedSeason ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, info = null) }
            when (val r = rosterRepository.addMember(teamId, season, userId, entryFee)) {
                is ApiResult.Success -> {
                    _ui.update {
                        it.copy(
                            saving = false,
                            info = if (entryFee != null) {
                                "Игрок добавлен, стартовый взнос начислен"
                            } else {
                                "Игрок добавлен в состав"
                            },
                        )
                    }
                    loadRoster(teamId, season)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(saving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun removeMember(userId: Long) {
        val teamId = teamManager.selected.value?.id ?: return
        val season = _ui.value.selectedSeason ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, info = null) }
            when (val r = rosterRepository.removeMember(teamId, season, userId)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(saving = false, info = "Игрок исключён из состава") }
                    loadRoster(teamId, season)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(saving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun replace(userIds: Set<Long>) {
        val teamId = teamManager.selected.value?.id ?: return
        val season = _ui.value.selectedSeason ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, info = null) }
            when (val r = rosterRepository.replace(teamId, season, userIds)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(saving = false, info = "Состав обновлён") }
                    loadRoster(teamId, season)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(saving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }
}
