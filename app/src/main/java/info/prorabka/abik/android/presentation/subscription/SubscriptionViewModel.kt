package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val teamManager: CurrentTeamManager,
    private val teamRepository: TeamRepository,
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    data class MemberBalance(
        val userId: Long,
        val name: String,
        val jerseyNumber: Int?,
        val balanceKopecks: Long,
        val status: MemberStatus,
    )

    data class UiState(
        val loading: Boolean = true,
        val noTeam: Boolean = false,
        val rows: List<MemberBalance> = emptyList(),
        val isManager: Boolean = false,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                if (team != null) {
                    load(team.id)
                } else {
                    _ui.update {
                        it.copy(loading = false, noTeam = true, rows = emptyList(), isManager = false)
                    }
                }
            }
        }
    }

    fun reload() {
        teamManager.selected.value?.let { load(it.id) }
    }

    private fun load(teamId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, noTeam = false, error = null) }

            var role: MemberRole? = null
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> role = d.data.myRole
                is ApiResult.HttpError -> _ui.update { it.copy(error = d.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }

            val members = (teamRepository.members(teamId) as? ApiResult.Success)?.data
                ?: emptyList()
            val balances = (financeRepository.balances(teamId) as? ApiResult.Success)?.data
                ?.associateBy { it.userId }
                ?: emptyMap()

            _ui.update {
                it.copy(
                    loading = false,
                    isManager = role == MemberRole.OWNER ||
                        role == MemberRole.MODERATOR ||
                        role == MemberRole.ADMIN,
                    rows = members
                        .sortedBy { m -> m.displayName }
                        .map { m ->
                            MemberBalance(
                                userId = m.userId,
                                name = m.displayName,
                                jerseyNumber = m.jerseyNumber,
                                balanceKopecks = balances[m.userId]?.amountKopecks ?: 0L,
                                status = m.status,
                            )
                        },
                )
            }
        }
    }
}
