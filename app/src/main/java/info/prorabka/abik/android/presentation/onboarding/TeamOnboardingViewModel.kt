package info.prorabka.abik.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.City
import info.prorabka.abik.android.domain.model.Invitation
import info.prorabka.abik.android.domain.model.Region
import info.prorabka.abik.android.domain.model.TeamCreateData
import info.prorabka.abik.android.domain.repository.GeoRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Онбординг команды: входящие приглашения + создание своей команды.
 */
@HiltViewModel
class TeamOnboardingViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val geoRepository: GeoRepository,
) : ViewModel() {

    companion object {
        /**
         * Заглушка логотипа: сервер требует logoUrl при создании команды,
         * а endpoint загрузки файлов появится позже (backend-api-extension.md, раздел 1).
         */
        const val DEFAULT_LOGO_URL = "default://logo"
    }

    data class UiState(
        val loading: Boolean = true,
        val invitations: List<Invitation> = emptyList(),
        val regions: List<Region> = emptyList(),
        val cities: List<City> = emptyList(),
        val selectedRegion: Region? = null,
        val selectedCity: City? = null,
        val name: String = "",
        val description: String = "",
        val season: String = "",
        val saving: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val inv = teamRepository.incomingInvitations()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(invitations = inv.data.filter { i -> i.status == "PENDING" })
                }
                else -> Unit
            }
            when (val r = geoRepository.regions()) {
                is ApiResult.Success -> _ui.update { it.copy(regions = r.data) }
                else -> Unit
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectRegion(region: Region) {
        _ui.update { it.copy(selectedRegion = region, selectedCity = null, cities = emptyList()) }
        viewModelScope.launch {
            when (val r = geoRepository.citiesByRegion(region.id)) {
                is ApiResult.Success -> _ui.update { it.copy(cities = r.data) }
                else -> Unit
            }
        }
    }

    fun selectCity(city: City) = _ui.update { it.copy(selectedCity = city) }
    fun onName(v: String) = _ui.update { it.copy(name = v) }
    fun onDescription(v: String) = _ui.update { it.copy(description = v) }
    fun onSeason(v: String) = _ui.update { it.copy(season = v) }

    fun accept(invitationId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            when (val r = teamRepository.acceptInvitation(invitationId)) {
                is ApiResult.Success -> onDone()
                is ApiResult.HttpError -> _ui.update { it.copy(saving = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun decline(invitationId: Long) {
        viewModelScope.launch {
            when (teamRepository.declineInvitation(invitationId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        invitations = it.invitations.filter { i -> i.id != invitationId },
                        info = "Приглашение отклонено",
                    )
                }
                else -> Unit
            }
        }
    }

    fun createTeam(onDone: () -> Unit) {
        val s = _ui.value
        when {
            s.name.isBlank() -> _ui.update { it.copy(error = "Укажите название команды") }
            s.selectedRegion == null -> _ui.update { it.copy(error = "Выберите домашний регион") }
            s.selectedCity == null -> _ui.update { it.copy(error = "Выберите город") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }
                val result = teamRepository.create(
                    TeamCreateData(
                        name = s.name.trim(),
                        cityId = s.selectedCity!!.id,
                        homeRegionId = s.selectedRegion!!.id,
                        logoUrl = DEFAULT_LOGO_URL,
                        description = s.description.ifBlank { null },
                        season = s.season.ifBlank { null },
                    ),
                )
                when (result) {
                    is ApiResult.Success -> onDone()
                    is ApiResult.HttpError -> _ui.update {
                        it.copy(saving = false, error = result.message)
                    }
                    is ApiResult.NetworkError -> _ui.update {
                        it.copy(saving = false, error = "Нет соединения с сервером")
                    }
                }
            }
        }
    }
}
