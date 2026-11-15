package info.prorabka.abik.android.presentation.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.domain.model.*
import info.prorabka.abik.android.domain.repository.AdminOperationsRepository
import info.prorabka.abik.android.domain.repository.AdminRepository
import info.prorabka.abik.android.domain.repository.FileRepository
import info.prorabka.abik.android.domain.repository.GeoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val legacy: AdminRepository,
    private val repository: AdminOperationsRepository,
    private val geoRepository: GeoRepository,
    private val files: FileRepository,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val adOnly: Boolean = false,
        val forbidden: Boolean = false,
        val overview: AdminOverview? = null,
        val users: List<AdminUserRow> = emptyList(),
        val teams: List<AdminTeamRow> = emptyList(),
        val targetTeams: List<AdminTarget> = emptyList(),
        val regions: List<Region> = emptyList(),
        val cities: List<City> = emptyList(),
        val messages: List<AdminMessageRow> = emptyList(),
        val registrationEvents: List<AdminRegistrationEvent> = emptyList(),
        val ads: List<Advertisement> = emptyList(),
        val selectedUser: AdminUserDetails? = null,
        val selectedTeam: AdminTeamDetails? = null,
        val selectedAdStats: AdStats? = null,
        val recipientCount: Long? = null,
        val filterRegionId: Long? = null,
        val filterCityId: Long? = null,
        val filterTeamId: Long? = null,
        val filterFrom: String? = null,
        val filterTo: String? = null,
        val legacyStats: AdminStats? = null,
        val uploadingImage: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    fun resolveUrl(url: String?) = urlResolver.resolve(url)
    fun dismissMessages() = _ui.update { it.copy(error = null, info = null) }

    fun load(adOnly: Boolean = _ui.value.adOnly) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, adOnly = adOnly, forbidden = false, error = null) }
            loadGeo()
            when (val targets = repository.targetTeams()) {
                is ApiResult.Success -> _ui.update { it.copy(targetTeams = targets.data) }
                is ApiResult.HttpError -> if (targets.httpCode == 403 && !adOnly) _ui.update { it.copy(forbidden = true) }
                else -> Unit
            }
            when (val result = repository.ads()) {
                is ApiResult.Success -> _ui.update { it.copy(ads = result.data) }
                is ApiResult.HttpError -> if (result.httpCode == 403) _ui.update { it.copy(forbidden = true) } else show(result)
                is ApiResult.NetworkError -> show(result)
            }
            if (!adOnly && !_ui.value.forbidden) {
                loadUsers()
                loadTeams()
                loadOverview()
                loadMessages()
                loadRegistrationEvents()
                when (val stats = legacy.stats()) {
                    is ApiResult.Success -> _ui.update { it.copy(legacyStats = stats.data) }
                    else -> Unit
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    private suspend fun loadGeo() {
        when (val result = geoRepository.regions()) {
            is ApiResult.Success -> _ui.update { it.copy(regions = result.data) }
            else -> Unit
        }
    }

    fun selectFilterRegion(regionId: Long?) {
        _ui.update { it.copy(filterRegionId = regionId, filterCityId = null, cities = emptyList()) }
        if (regionId != null) viewModelScope.launch {
            when (val result = geoRepository.citiesByRegion(regionId)) {
                is ApiResult.Success -> _ui.update { it.copy(cities = result.data) }
                else -> Unit
            }
        }
    }

    fun loadMessageCities(regionId: Long?) = selectFilterRegion(regionId)

    fun applyFilters(from: String?, to: String?, regionId: Long?, cityId: Long?, teamId: Long?) {
        _ui.update { it.copy(filterFrom = from, filterTo = to, filterRegionId = regionId,
            filterCityId = cityId, filterTeamId = teamId) }
        loadOverview()
    }

    fun loadOverview() {
        val state = _ui.value
        viewModelScope.launch {
            when (val result = repository.overview(state.filterFrom, state.filterTo, state.filterRegionId,
                state.filterCityId, state.filterTeamId)) {
                is ApiResult.Success -> _ui.update { it.copy(overview = result.data) }
                else -> show(result)
            }
        }
    }

    private suspend fun loadUsers() {
        when (val result = repository.users()) {
            is ApiResult.Success -> _ui.update { it.copy(users = result.data) }
            else -> show(result)
        }
    }
    private suspend fun loadTeams() {
        when (val result = repository.teams()) {
            is ApiResult.Success -> _ui.update { it.copy(teams = result.data) }
            else -> show(result)
        }
    }
    private suspend fun loadMessages() {
        when (val result = repository.messages()) {
            is ApiResult.Success -> _ui.update { it.copy(messages = result.data) }
            else -> show(result)
        }
    }
    fun loadRegistrationEvents(status: String? = null) {
        viewModelScope.launch {
            when (val result = repository.registrationEvents(status)) {
                is ApiResult.Success -> _ui.update { it.copy(registrationEvents = result.data) }
                else -> show(result)
            }
        }
    }

    fun userDetails(id: Long) = viewModelScope.launch {
        when (val result = repository.userDetails(id)) {
            is ApiResult.Success -> _ui.update { it.copy(selectedUser = result.data) }
            else -> show(result)
        }
    }
    fun teamDetails(id: Long) = viewModelScope.launch {
        when (val result = repository.teamDetails(id)) {
            is ApiResult.Success -> _ui.update { it.copy(selectedTeam = result.data) }
            else -> show(result)
        }
    }
    fun closeDetails() = _ui.update { it.copy(selectedUser = null, selectedTeam = null, selectedAdStats = null) }

    fun blockUser(id: Long, reason: String) = action(repositoryCall = { repository.blockUser(id, reason) }, success = "Пользователь заблокирован") { load() }
    fun unblockUser(id: Long) = action(repositoryCall = { repository.unblockUser(id) }, success = "Пользователь разблокирован") { load() }
    fun setAdAdmin(id: Long, value: Boolean) = action(repositoryCall = { repository.setAdAdmin(id, value) },
        success = if (value) "Доступ к рекламе выдан" else "Доступ к рекламе отозван") { load() }
    fun blockTeam(id: Long, reason: String) = action(repositoryCall = { repository.blockTeam(id, reason) }, success = "Команда заблокирована") { load() }
    fun unblockTeam(id: Long) = action(repositoryCall = { repository.unblockTeam(id) }, success = "Команда разблокирована") { load() }

    fun recipientCount(draft: AdminMessageDraft) = viewModelScope.launch {
        when (val result = repository.recipientCount(draft)) {
            is ApiResult.Success -> _ui.update { it.copy(recipientCount = result.data) }
            else -> show(result)
        }
    }
    fun sendMessage(draft: AdminMessageDraft) = action(repositoryCall = { repository.sendMessage(draft) }, success = "Сообщение отправлено") {
        _ui.update { it.copy(recipientCount = null) }
        loadMessages()
    }

    fun saveAd(draft: AdvertisementDraft) = action(repositoryCall = { repository.saveAd(draft) },
        success = if (draft.id == null) "Публикация создана" else "Публикация сохранена") { reloadAds() }
    fun setAdActive(id: Long, active: Boolean) = action(repositoryCall = { repository.setAdActive(id, active) },
        success = if (active) "Публикация включена" else "Публикация выключена") { reloadAds() }
    fun deleteAd(id: Long) = action(repositoryCall = { repository.deleteAd(id) }, success = "Публикация удалена") { reloadAds() }
    private suspend fun reloadAds() {
        when (val result = repository.ads()) {
            is ApiResult.Success -> _ui.update { it.copy(ads = result.data) }
            else -> show(result)
        }
    }
    fun adStats(id: Long) = viewModelScope.launch {
        when (val result = repository.adStats(id)) {
            is ApiResult.Success -> _ui.update { it.copy(selectedAdStats = result.data) }
            else -> show(result)
        }
    }

    fun uploadAdImage(uri: Uri, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(uploadingImage = true) }
            when (val result = files.upload(uri)) {
                is ApiResult.Success -> onReady(result.data)
                else -> show(result)
            }
            _ui.update { it.copy(uploadingImage = false) }
        }
    }

    fun createRegion(name: String, code: String, timezone: String) = viewModelScope.launch {
        when (val result = legacy.createRegion(name, code, timezone)) {
            is ApiResult.Success -> { _ui.update { it.copy(info = "Регион добавлен") }; loadGeo() }
            else -> show(result)
        }
    }
    fun createCity(regionId: Long, name: String) = viewModelScope.launch {
        when (val result = legacy.createCity(regionId, name)) {
            is ApiResult.Success -> _ui.update { it.copy(info = "Город добавлен") }
            else -> show(result)
        }
    }
    fun publishRelease(release: AppRelease) = viewModelScope.launch {
        when (val result = legacy.publishRelease(release)) {
            is ApiResult.Success -> _ui.update { it.copy(info = "Версия ${release.versionName} опубликована", error = null) }
            else -> show(result)
        }
    }

    private fun action(repositoryCall: suspend () -> ApiResult<Unit>, success: String, after: suspend () -> Unit) {
        viewModelScope.launch {
            when (val result = repositoryCall()) {
                is ApiResult.Success -> { _ui.update { it.copy(info = success, error = null) }; after() }
                else -> show(result)
            }
        }
    }

    private fun show(result: ApiResult<*>) {
        when (result) {
            is ApiResult.HttpError -> _ui.update { it.copy(error = result.message, info = null) }
            is ApiResult.NetworkError -> _ui.update { it.copy(error = "Нет соединения с сервером", info = null) }
            is ApiResult.Success -> Unit
        }
    }
}
