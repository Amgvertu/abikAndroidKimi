package info.prorabka.abik.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.City
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.Region
import info.prorabka.abik.android.domain.model.StickHand
import info.prorabka.abik.android.domain.repository.GeoRepository
import info.prorabka.abik.android.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Форма профиля. Используется и в онбординге (обязательное заполнение),
 * и при редактировании профиля (prefill = true).
 */
@HiltViewModel
class ProfileFormViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val geoRepository: GeoRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null,
        val regions: List<Region> = emptyList(),
        val cities: List<City> = emptyList(),
        val selectedRegion: Region? = null,
        val selectedCity: City? = null,
        val lastName: String = "",
        val firstName: String = "",
        val middleName: String = "",
        val birthDate: LocalDate? = null,
        val height: String = "",
        val weight: String = "",
        val stickHand: StickHand = StickHand.RIGHT,
        val positions: Set<Position> = emptySet(),
        val jerseySize: String = "",
        val coversSize: String = "",
        val gaitersSize: String = "",
        val avatarUrl: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(prefill: Boolean) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val r = geoRepository.regions()) {
                is ApiResult.Success -> _ui.update { it.copy(regions = r.data) }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            if (prefill) {
                when (val p = userRepository.myProfile()) {
                    is ApiResult.Success -> {
                        val profile = p.data
                        _ui.update {
                            it.copy(
                                lastName = profile.lastName,
                                firstName = profile.firstName,
                                middleName = profile.middleName,
                                birthDate = profile.birthDate,
                                height = profile.heightCm.toString(),
                                weight = profile.weightKg.toString(),
                                stickHand = profile.stickHand,
                                positions = profile.positions,
                                jerseySize = profile.jerseySize.orEmpty(),
                                coversSize = profile.coversSize.orEmpty(),
                                gaitersSize = profile.gaitersSize.orEmpty(),
                                avatarUrl = profile.avatarUrl,
                            )
                        }
                        // Восстановить выбранные регион/город
                        val region = _ui.value.regions.firstOrNull { it.id == profile.regionId }
                        if (region != null) {
                            _ui.update { it.copy(selectedRegion = region) }
                            loadCities(region.id, preselectCityId = profile.cityId)
                        }
                    }
                    else -> Unit
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun selectRegion(region: Region) {
        _ui.update { it.copy(selectedRegion = region, selectedCity = null, cities = emptyList()) }
        loadCities(region.id, preselectCityId = null)
    }

    private fun loadCities(regionId: Long, preselectCityId: Long?) {
        viewModelScope.launch {
            when (val r = geoRepository.citiesByRegion(regionId)) {
                is ApiResult.Success -> _ui.update { state ->
                    state.copy(
                        cities = r.data,
                        selectedCity = preselectCityId?.let { id ->
                            r.data.firstOrNull { it.id == id }
                        } ?: state.selectedCity,
                    )
                }
                else -> Unit
            }
        }
    }

    fun selectCity(city: City) = _ui.update { it.copy(selectedCity = city) }
    fun onLastName(v: String) = _ui.update { it.copy(lastName = v) }
    fun onFirstName(v: String) = _ui.update { it.copy(firstName = v) }
    fun onMiddleName(v: String) = _ui.update { it.copy(middleName = v) }
    fun onBirthDate(v: LocalDate) = _ui.update { it.copy(birthDate = v) }
    fun onHeight(v: String) = _ui.update { it.copy(height = v.filter(Char::isDigit).take(3)) }
    fun onWeight(v: String) = _ui.update { it.copy(weight = v.filter(Char::isDigit).take(3)) }
    fun onStickHand(v: StickHand) = _ui.update { it.copy(stickHand = v) }
    fun togglePosition(p: Position) = _ui.update {
        it.copy(
            positions = if (p in it.positions) it.positions - p else it.positions + p,
        )
    }
    fun onJerseySize(v: String) = _ui.update { it.copy(jerseySize = v.take(20)) }
    fun onCoversSize(v: String) = _ui.update { it.copy(coversSize = v.take(20)) }
    fun onGaitersSize(v: String) = _ui.update { it.copy(gaitersSize = v.take(20)) }

    fun save(onSuccess: () -> Unit) {
        val s = _ui.value
        val height = s.height.toIntOrNull()
        val weight = s.weight.toIntOrNull()
        when {
            s.selectedCity == null -> _ui.update { it.copy(error = "Выберите регион и город") }
            s.lastName.isBlank() -> _ui.update { it.copy(error = "Укажите фамилию") }
            s.firstName.isBlank() -> _ui.update { it.copy(error = "Укажите имя") }
            s.middleName.isBlank() -> _ui.update { it.copy(error = "Укажите отчество") }
            s.birthDate == null -> _ui.update { it.copy(error = "Укажите дату рождения") }
            height == null || height !in 100..250 ->
                _ui.update { it.copy(error = "Рост — от 100 до 250 см") }
            weight == null || weight !in 30..200 ->
                _ui.update { it.copy(error = "Вес — от 30 до 200 кг") }
            s.positions.isEmpty() -> _ui.update { it.copy(error = "Укажите хотя бы одно амплуа") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }
                val result = userRepository.updateMyProfile(
                    cityId = s.selectedCity!!.id,
                    lastName = s.lastName.trim(),
                    firstName = s.firstName.trim(),
                    middleName = s.middleName.trim(),
                    birthDateIso = s.birthDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    heightCm = height!!,
                    weightKg = weight!!,
                    stickHand = s.stickHand,
                    positions = s.positions,
                    jerseySize = s.jerseySize.ifBlank { null },
                    coversSize = s.coversSize.ifBlank { null },
                    gaitersSize = s.gaitersSize.ifBlank { null },
                    avatarUrl = s.avatarUrl,
                )
                when (result) {
                    is ApiResult.Success -> onSuccess()
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
