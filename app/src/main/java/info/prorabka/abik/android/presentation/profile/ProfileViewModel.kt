package info.prorabka.abik.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.prefs.SettingsStore
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.Profile
import info.prorabka.abik.android.domain.model.TeamMember
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
    private val teamManager: CurrentTeamManager,
    private val settingsStore: SettingsStore,
    private val tokenStore: TokenStore,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val profile: Profile? = null,
        val phone: String = "",
        val jerseyNumber: Int? = null,
        val isSuperAdmin: Boolean = false,
        val error: String? = null,
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
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun useRemoteServer() {
        viewModelScope.launch { settingsStore.setBaseUrlOverride(null) }
    }

    fun useLocalServer() {
        viewModelScope.launch { settingsStore.setBaseUrlOverride(BuildConfig.LOCAL_BASE_URL) }
    }
}
