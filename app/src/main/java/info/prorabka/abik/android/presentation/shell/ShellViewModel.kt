package info.prorabka.abik.android.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.repository.AuthRepository
import info.prorabka.abik.android.domain.repository.DeviceRepository
import info.prorabka.abik.android.domain.repository.FinanceRepository
import info.prorabka.abik.android.domain.repository.NotificationRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import info.prorabka.abik.android.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Состояние главной оболочки, командные данные и административные роли. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    val teamManager: CurrentTeamManager,
    private val teamRepository: TeamRepository,
    private val financeRepository: FinanceRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
    private val tokenStore: TokenStore,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    data class UiState(
        val teams: List<Team> = emptyList(),
        val selected: Team? = null,
        val myRole: MemberRole? = null,
        val myIsTrainer: Boolean = false,
        val myBalanceKopecks: Long? = null,
        val teamBalanceKopecks: Long? = null,
        val unreadNotifications: Int = 0,
        val isSuperAdmin: Boolean = false,
        val isAdAdmin: Boolean = false,
        val invitations: List<Invitation> = emptyList(),
        val invitationSaving: Boolean = false,
        val invitationError: String? = null,
        val loading: Boolean = true,
    ) {
        val isManager: Boolean
            get() = myRole == MemberRole.OWNER || myRole == MemberRole.MODERATOR || myRole == MemberRole.ADMIN
        val isOwnerOrAdmin: Boolean
            get() = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch { teamManager.teams.collect { teams -> _ui.update { it.copy(teams = teams) } } }
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                _ui.update { it.copy(selected = team) }
                if (team != null) loadTeamData(team.id)
            }
        }
        refresh()
        registerDevice()
        startUnreadPolling()
    }

    private fun registerDevice() { viewModelScope.launch { deviceRepository.registerCurrentDevice() } }

    private fun startUnreadPolling() {
        viewModelScope.launch {
            while (isActive) {
                refreshUnreadCount()
                delay(UNREAD_POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.unreadCount()) {
                is ApiResult.Success -> _ui.update { it.copy(unreadNotifications = result.data) }
                else -> Unit
            }
        }
    }

    private companion object { const val UNREAD_POLL_INTERVAL_MS = 30_000L }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            when (val me = userRepository.me()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(isSuperAdmin = me.data.isSuperAdmin, isAdAdmin = me.data.isAdAdmin)
                }
                else -> Unit
            }
            teamManager.refresh()
            teamManager.selected.value?.let { loadTeamData(it.id) }
            loadInvitations()
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectTeam(team: Team) = teamManager.select(team)

    private suspend fun loadInvitations() {
        when (val result = teamRepository.incomingInvitations()) {
            is ApiResult.Success -> _ui.update {
                it.copy(invitations = result.data.filter { invitation -> invitation.status == "PENDING" })
            }
            else -> Unit
        }
    }

    fun acceptInvitation(invitation: Invitation) {
        if (_ui.value.invitationSaving) return
        viewModelScope.launch {
            _ui.update { it.copy(invitationSaving = true, invitationError = null) }
            when (val result = teamRepository.acceptInvitation(invitation.id)) {
                is ApiResult.Success -> {
                    teamManager.refresh()
                    teamManager.teams.value.firstOrNull { it.id == invitation.teamId }
                        ?.let(teamManager::select)
                    _ui.update {
                        it.copy(
                            invitationSaving = false,
                            invitations = it.invitations.filter { item -> item.id != invitation.id },
                        )
                    }
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(invitationSaving = false, invitationError = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(invitationSaving = false, invitationError = "Нет соединения с сервером")
                }
            }
        }
    }

    fun declineInvitation(invitation: Invitation) {
        if (_ui.value.invitationSaving) return
        viewModelScope.launch {
            _ui.update { it.copy(invitationSaving = true, invitationError = null) }
            when (val result = teamRepository.declineInvitation(invitation.id)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        invitationSaving = false,
                        invitations = it.invitations.filter { item -> item.id != invitation.id },
                    )
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(invitationSaving = false, invitationError = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(invitationSaving = false, invitationError = "Нет соединения с сервером")
                }
            }
        }
    }

    private suspend fun loadTeamData(teamId: Long) {
        when (val d = teamRepository.detail(teamId)) {
            is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
            else -> Unit
        }
        when (val members = teamRepository.members(teamId)) {
            is ApiResult.Success -> {
                val myId = tokenStore.userId
                _ui.update { it.copy(myIsTrainer = members.data.firstOrNull { m -> m.userId == myId }?.isTrainer == true) }
            }
            else -> Unit
        }
        when (val b = financeRepository.balances(teamId)) {
            is ApiResult.Success -> {
                val myId = tokenStore.userId
                _ui.update {
                    it.copy(myBalanceKopecks = b.data.firstOrNull { bal -> bal.userId == myId }?.amountKopecks,
                        teamBalanceKopecks = b.data.sumOf { bal -> bal.amountKopecks })
                }
            }
            else -> Unit
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            teamManager.clear()
            _ui.update { UiState(loading = false) }
            onLoggedOut()
        }
    }
}
