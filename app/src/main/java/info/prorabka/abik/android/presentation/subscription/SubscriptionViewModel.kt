package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import info.prorabka.abik.android.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val teamManager: CurrentTeamManager,
    private val teamRepository: TeamRepository,
    private val financeRepository: FinanceRepository,
    private val userRepository: UserRepository,
    private val urlResolver: UrlResolver,
    private val tokenStore: TokenStore,
) : ViewModel() {

    data class MemberBalance(
        val userId: Long,
        val name: String,
        val jerseyNumber: Int?,
        val balanceKopecks: Long?,
        val status: MemberStatus,
        val avatarUrl: String? = null,
    )

    data class UiState(
        val loading: Boolean = true,
        val noTeam: Boolean = false,
        val rows: List<MemberBalance> = emptyList(),
        val isManager: Boolean = false,
        val isOwner: Boolean = false,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    private var loadJob: Job? = null
    private var loadingTeamId: Long? = null
    private var resumeSeen = false

    init {
        viewModelScope.launch {
            teamManager.selected
                .map { it?.id }
                .distinctUntilChanged()
                .collect { teamId ->
                    if (teamId != null) {
                        load(teamId)
                    } else {
                        loadJob?.cancel()
                        loadingTeamId = null
                        _ui.update {
                            it.copy(loading = false, noTeam = true, rows = emptyList(), isManager = false)
                        }
                    }
                }
        }
    }

    fun reloadOnResume() {
        if (!resumeSeen) {
            resumeSeen = true
            return
        }
        reload()
    }

    fun reload() {
        teamManager.selected.value?.let { load(it.id) }
    }

    private fun load(teamId: Long) {
        // init (подписка на команду) и LifecycleResumeEffect стартуют вместе.
        // Повторный вызов для уже загружаемой команды игнорируем.
        if (loadJob?.isActive == true && loadingTeamId == teamId) return
        loadJob?.cancel()
        loadingTeamId = teamId
        loadJob = viewModelScope.launch {
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
            val isManager = role == MemberRole.OWNER ||
                role == MemberRole.MODERATOR ||
                role == MemberRole.ADMIN
            val myUserId = if (isManager) {
                null
            } else {
                (userRepository.me() as? ApiResult.Success)?.data?.id
                    ?: tokenStore.userId.takeIf { it > 0 }
            }

            _ui.update {
                it.copy(
                    loading = false,
                    isManager = isManager,
                    isOwner = role == MemberRole.OWNER,
                    rows = members
                        .filter { m -> isManager || m.userId == myUserId }
                        .sortedBy { m -> m.displayName }
                        .map { m ->
                            MemberBalance(
                                userId = m.userId,
                                name = m.displayName,
                                jerseyNumber = m.jerseyNumber,
                                balanceKopecks = balances?.get(m.userId)?.amountKopecks,
                                status = m.status,
                                avatarUrl = urlResolver.resolve(m.avatarUrl),
                            )
                        },
                )
            }
        }
    }
}
