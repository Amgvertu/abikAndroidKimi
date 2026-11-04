package info.prorabka.abik.android.presentation.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.BuildConfig
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.RealtimeManager
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.model.AppRelease
import info.prorabka.abik.android.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSystemViewModel @Inject constructor(
    private val releaseRepository: ReleaseRepository,
    private val realtime: RealtimeManager,
    private val tokenStore: TokenStore,
    teamManager: CurrentTeamManager,
) : ViewModel() {
    private val _update = MutableStateFlow<AppRelease?>(null)
    val update: StateFlow<AppRelease?> = _update
    private var checked = false

    init {
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                if (team != null) {
                    realtime.start()
                    checkUpdate()
                }
            }
        }
    }

    fun onStart() {
        if (tokenStore.isLoggedIn) realtime.start()
        checkUpdate()
    }

    fun onStop() = realtime.stop()

    fun checkUpdate() {
        if (checked || !tokenStore.isLoggedIn) return
        checked = true
        viewModelScope.launch {
            when (val result = releaseRepository.latest()) {
                is ApiResult.Success -> if (result.data.versionCode > BuildConfig.VERSION_CODE) {
                    _update.value = result.data
                }
                else -> Unit
            }
        }
    }

    fun dismissUpdate() {
        if (_update.value?.mandatory != true) _update.value = null
    }
}
