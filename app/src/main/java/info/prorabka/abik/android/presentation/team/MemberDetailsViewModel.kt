package info.prorabka.abik.android.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Profile
import info.prorabka.abik.android.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberDetailsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val profile: Profile? = null,
        val jerseyNumber: Int? = null,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(userId: Long) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = userRepository.profileOf(userId)) {
                is ApiResult.Success -> _ui.update { it.copy(profile = r.data, loading = false) }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
}
