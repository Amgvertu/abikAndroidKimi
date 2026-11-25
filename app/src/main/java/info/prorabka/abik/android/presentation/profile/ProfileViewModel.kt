package info.prorabka.abik.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.LocalServerAvailability
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.core.prefs.SettingsStore
import info.prorabka.abik.android.core.prefs.ThemeMode
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.NotificationSettings
import info.prorabka.abik.android.domain.model.Profile
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.FilePurpose
import info.prorabka.abik.android.domain.repository.FileRepository
import info.prorabka.abik.android.domain.repository.NotificationRepository
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
    private val notificationRepository: NotificationRepository,
    private val teamManager: CurrentTeamManager,
    private val settingsStore: SettingsStore,
    private val tokenStore: TokenStore,
    private val urlResolver: UrlResolver,
    private val biometricAuthManager: info.prorabka.abik.android.core.security.BiometricAuthManager,
    private val localServerAvailability: LocalServerAvailability,
) : ViewModel() {

    fun resolveUrl(url: String?): String? = urlResolver.resolve(url)

    private val _biometricEnabled = MutableStateFlow(tokenStore.biometricEnabled)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled
    val biometricAvailable: Boolean get() = biometricAuthManager.canAuthenticate()

    data class UiState(
        val loading: Boolean = true,
        val profile: Profile? = null,
        val phone: String = "",
        val jerseyNumber: Int? = null,
        val isSuperAdmin: Boolean = false,
        val notificationSettings: NotificationSettings = NotificationSettings(),
        val notificationSettingsSaving: Boolean = false,
        val notificationSettingsError: String? = null,
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val isDebugBuild: Boolean = BuildConfig.DEBUG
    val localServerAvailable = localServerAvailability.available

    init {
        load()
        loadNotificationSettings()
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

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            when (val result = notificationRepository.settings()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(notificationSettings = result.data, notificationSettingsError = null)
                }
                is ApiResult.HttpError -> _ui.update { it.copy(notificationSettingsError = result.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(notificationSettingsError = "Нет соединения с сервером")
                }
            }
        }
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            _ui.update { it.copy(notificationSettingsSaving = true, notificationSettingsError = null) }
            when (val result = notificationRepository.saveSettings(settings)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        notificationSettings = result.data,
                        notificationSettingsSaving = false,
                        notificationSettingsError = null,
                        info = "Настройки напоминаний сохранены",
                    )
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(notificationSettingsSaving = false, notificationSettingsError = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(
                        notificationSettingsSaving = false,
                        notificationSettingsError = "Нет соединения с сервером",
                    )
                }
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        tokenStore.biometricEnabled = enabled
        _biometricEnabled.value = enabled
    }

    /** Загрузить новый аватар: файл → /api/v1/files → PUT профиля. */
    fun uploadAvatar(uri: android.net.Uri) {
        val profile = _ui.value.profile ?: return
        viewModelScope.launch {
            when (val uploaded = fileRepository.upload(uri, FilePurpose.AVATAR)) {
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
                playerStatus = profile.playerStatus,
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
        if (!BuildConfig.DEBUG || !localServerAvailability.available.value) return
        // Через геттер SettingsStore, а не константу BuildConfig — иначе kotlinc
        // инлайнит значение на момент компиляции и может зашить протухший URL.
        viewModelScope.launch { settingsStore.setBaseUrlOverride(settingsStore.localBaseUrl) }
    }
}
