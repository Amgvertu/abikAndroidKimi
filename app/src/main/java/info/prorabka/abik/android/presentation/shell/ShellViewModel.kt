package info.prorabka.abik.android.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.repository.AuthRepository
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Состояние главной оболочки: команды, выбранная команда,
 * моя роль и мой баланс для тулбара.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    val teamManager: CurrentTeamManager,
    private val teamRepository: TeamRepository,
    private val financeRepository: FinanceRepository,
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    /** Относительный URL файла → абсолютный (для логотипов/аватаров). */
    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    data class UiState(
        val teams: List<Team> = emptyList(),
        val selected: Team? = null,
        val myRole: MemberRole? = null,
        val myBalanceKopecks: Long? = null,
        val teamBalanceKopecks: Long? = null,
        val loading: Boolean = true,
    ) {
        val isManager: Boolean
            get() = myRole == MemberRole.OWNER ||
                myRole == MemberRole.MODERATOR ||
                myRole == MemberRole.ADMIN
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            teamManager.teams.collect { teams -> _ui.update { it.copy(teams = teams) } }
        }
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                _ui.update { it.copy(selected = team) }
                if (team != null) loadTeamData(team.id)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            teamManager.refresh()
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectTeam(team: Team) = teamManager.select(team)

    private suspend fun loadTeamData(teamId: Long) {
        when (val d = teamRepository.detail(teamId)) {
            is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
            else -> Unit
        }
        when (val b = financeRepository.balances(teamId)) {
            is ApiResult.Success -> {
                val myId = tokenStore.userId
                _ui.update {
                    it.copy(
                        myBalanceKopecks = b.data.firstOrNull { bal -> bal.userId == myId }
                            ?.amountKopecks,
                        teamBalanceKopecks = b.data.sumOf { bal -> bal.amountKopecks },
                    )
                }
            }
            else -> Unit
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
