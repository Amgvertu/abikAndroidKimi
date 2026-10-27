package info.prorabka.abik.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.prefs.SettingsStore
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.Profile
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.FileRepository
import info.prorabka.abik.android.domain.repository.UserRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val fileRepository: FileRepository,
    private val teamManager: CurrentTeamManager,
    private val settingsStore: SettingsStore,
    private val tokenStore: TokenStore,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    data class UiState(
        val loading: Boolean = true,
        val profile: Profile? = null,
        val phone: String = "",
        val jerseyNumber: Int? = null,
        val isSuperAdmin: Boolean = false,
        val invitations: List<Invitation> = emptyList(),
        val invitationSaving: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val isDebugBuild: Boolean = BuildConfig.DEBUG
    val currentBaseUrl: StateFlow<String> = MutableStateFlow(BuildConfig.DEFAULT_BASE_URL)

    init {
        load()
        viewModelScope.launch {
            settingsStore.baseUrlOverride.collect { override ->
                (currentBaseUrl as MutableStateFlow).value =
                    override ?: BuildConfig.DEFAULT_BASE_URL
            }
        }
        // Игровой номер — из состава текущей команды
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                if (team != null) {
                    when (val m = teamRepository.members(team.id)) {
                        is ApiResult.Success -> {
                            val me = m.data.firstOrNull { it.userId == tokenStore.userId }
                            _ui.update { it.copy(jerseyNumber = me?.jerseyNumber) }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val me = userRepository.me()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(phone = me.data.phone, isSuperAdmin = me.data.isSuperAdmin)
                }
                else -> Unit
            }
            when (val r = userRepository.myProfile()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(profile = r.data, loading = false)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
            when (val inv = teamRepository.incomingInvitations()) {
                is ApiResult.Success -> _ui.update { it.copy(invitations = inv.data) }
                else -> Unit
            }
        }
    }

    /** Принять приглашение: обновить команды и переключиться на новую. */
    fun acceptInvitation(invitation: Invitation) {
        viewModelScope.launch {
            _ui.update { it.copy(invitationSaving = true, error = null, info = null) }
            when (val r = teamRepository.acceptInvitation(invitation.id)) {
                is ApiResult.Success -> {
                    teamManager.refresh()
                    teamManager.teams.value
                        .firstOrNull { it.id == invitation.teamId }
                        ?.let(teamManager::select)
                    _ui.update {
                        it.copy(
                            invitationSaving = false,
                            info = "Вы вступили в команду «${invitation.teamName}»",
                        )
                    }
                    load()
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(invitationSaving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(invitationSaving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun declineInvitation(invitation: Invitation) {
        viewModelScope.launch {
            _ui.update { it.copy(invitationSaving = true, error = null, info = null) }
            when (val r = teamRepository.declineInvitation(invitation.id)) {
                is ApiResult.Success -> {
                    _ui.update {
                        it.copy(
                            invitationSaving = false,
                            invitations = it.invitations.filter { i -> i.id != invitation.id },
                            info = "Приглашение отклонено",
                        )
                    }
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(invitationSaving = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(invitationSaving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    /** Загрузить новый аватар: файл → /api/v1/files → PUT профиля. */
    fun uploadAvatar(uri: android.net.Uri) {
        val profile = _ui.value.profile ?: return
        viewModelScope.launch {
            when (val uploaded = fileRepository.upload(uri)) {
                is ApiResult.Success -> saveAvatar(profile, uploaded.data)
                is ApiResult.HttpError -> _ui.update { it.copy(error = uploaded.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    /** Удалить аватар (avatarUrl = null). */
    fun removeAvatar() {
        val profile = _ui.value.profile ?: return
        viewModelScope.launch { saveAvatar(profile, null) }
    }

    private suspend fun saveAvatar(profile: Profile, avatarUrl: String?) {
        when (
            val result = userRepository.updateMyProfile(
                cityId = profile.cityId,
                lastName = profile.lastName,
                firstName = profile.firstName,
                middleName = profile.middleName,
                birthDateIso = profile.birthDate
                    ?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
                heightCm = profile.heightCm,
                weightKg = profile.weightKg,
                stickHand = profile.stickHand,
                positions = profile.positions,
                jerseySize = profile.jerseySize,
                coversSize = profile.coversSize,
                gaitersSize = profile.gaitersSize,
                avatarUrl = avatarUrl,
            )
        ) {
            is ApiResult.Success -> _ui.update { it.copy(profile = result.data, error = null) }
            is ApiResult.HttpError -> _ui.update { it.copy(error = result.message) }
            is ApiResult.NetworkError -> _ui.update {
                it.copy(error = "Нет соединения с сервером")
            }
        }
    }

    fun useRemoteServer() {
        viewModelScope.launch { settingsStore.setBaseUrlOverride(null) }
    }

    fun useLocalServer() {
        // Через геттер SettingsStore, а не константу BuildConfig — иначе kotlinc
        // инлайнит значение на момент компиляции и может зашить протухший URL.
        viewModelScope.launch { settingsStore.setBaseUrlOverride(settingsStore.localBaseUrl) }
    }
}
