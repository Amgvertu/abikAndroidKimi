package info.prorabka.abik.android.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.Region
import info.prorabka.abik.android.domain.model.Team
import info.prorabka.abik.android.domain.model.User
import info.prorabka.abik.android.domain.repository.AdminRepository
import info.prorabka.abik.android.domain.repository.GeoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val geoRepository: GeoRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val users: List<User> = emptyList(),
        val teams: List<Team> = emptyList(),
        val regions: List<Region> = emptyList(),
        val forbidden: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val u = adminRepository.listUsers()) {
                is ApiResult.Success -> _ui.update { it.copy(users = u.data) }
                is ApiResult.HttpError -> {
                    if (u.httpCode == 403) {
                        _ui.update { it.copy(forbidden = true) }
                    } else {
                        _ui.update { it.copy(error = u.message) }
                    }
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            when (val t = adminRepository.listTeams()) {
                is ApiResult.Success -> _ui.update { it.copy(teams = t.data) }
                else -> Unit
            }
            when (val r = geoRepository.regions()) {
                is ApiResult.Success -> _ui.update { it.copy(regions = r.data) }
                else -> Unit
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun toggleBlock(user: User) {
        viewModelScope.launch {
            val result = if (user.status == "BLOCKED") {
                adminRepository.unblockUser(user.id)
            } else {
                adminRepository.blockUser(user.id)
            }
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.HttpError -> _ui.update { it.copy(error = result.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun createRegion(name: String, code: String, timezone: String) {
        viewModelScope.launch {
            when (val r = adminRepository.createRegion(name, code, timezone)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(info = "Регион добавлен") }
                    when (val regions = geoRepository.regions()) {
                        is ApiResult.Success -> _ui.update { it.copy(regions = regions.data) }
                        else -> Unit
                    }
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun createCity(regionId: Long, name: String) {
        viewModelScope.launch {
            when (val r = adminRepository.createCity(regionId, name)) {
                is ApiResult.Success -> _ui.update { it.copy(info = "Город добавлен") }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }
}
