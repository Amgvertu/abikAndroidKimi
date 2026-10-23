package info.prorabka.abik.android.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.prefs.TokenStore
import info.prorabka.abik.android.domain.repository.TeamRepository
import info.prorabka.abik.android.domain.repository.UserRepository
import info.prorabka.abik.android.presentation.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Сплэш: решает, куда идти — вход, заполнение профиля,
 * создание/вступление в команду или главный экран.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination

    init {
        resolve()
    }

    fun resolve() {
        _destination.value = null
        viewModelScope.launch {
            if (!tokenStore.isLoggedIn) {
                _destination.value = Routes.LOGIN
                return@launch
            }
            when (val profile = userRepository.myProfile()) {
                is ApiResult.Success -> routeByTeams()
                is ApiResult.HttpError -> {
                    // 404 — профиль ещё не заполнен
                    if (profile.httpCode == 404) {
                        _destination.value = Routes.ONBOARDING_PROFILE
                    } else {
                        // Сервер недоступен/ошибка — пробуем в main, там покажем ошибку
                        _destination.value = Routes.MAIN
                    }
                }
                is ApiResult.NetworkError -> _destination.value = Routes.MAIN
            }
        }
    }

    private suspend fun routeByTeams() {
        when (val teams = teamRepository.myTeams()) {
            is ApiResult.Success ->
                _destination.value = if (teams.data.isEmpty()) Routes.ONBOARDING_TEAM else Routes.MAIN
            else -> _destination.value = Routes.MAIN
        }
    }
}
