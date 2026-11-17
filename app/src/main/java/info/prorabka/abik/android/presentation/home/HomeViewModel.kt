package info.prorabka.abik.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.network.UrlResolver
import info.prorabka.abik.android.domain.model.Advertisement
import info.prorabka.abik.android.domain.model.AppNotification
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.model.SubscriptionPoll
import info.prorabka.abik.android.domain.model.SubscriptionPollAnswer
import info.prorabka.abik.android.domain.repository.AdminOperationsRepository
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.EventRepository
import info.prorabka.abik.android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val teamManager: CurrentTeamManager,
    private val eventRepository: EventRepository,
    private val notificationRepository: NotificationRepository,
    private val chargeRepository: ChargeRepository,
    private val advertising: AdminOperationsRepository,
    private val urlResolver: UrlResolver,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val nearestEvent: EventListItem? = null,
        val notifications: List<AppNotification> = emptyList(),
        val advertisement: Advertisement? = null,
        val subscriptionPoll: SubscriptionPoll? = null,
        val pollVoting: Boolean = false,
        val pollError: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    private val sessionId = UUID.randomUUID().toString()
    private var currentTeamId: Long? = null
    fun resolveUrl(url: String?) = urlResolver.resolve(url)

    init {
        viewModelScope.launch {
            teamManager.selected.collect { team ->
                currentTeamId = team?.id
                if (team != null) load(team.id) else _ui.update {
                    it.copy(loading = false, nearestEvent = null, notifications = emptyList(),
                        advertisement = null, subscriptionPoll = null)
                }
            }
        }
    }

    fun refresh() { currentTeamId?.let { teamId -> viewModelScope.launch { load(teamId) } } }

    fun voteSubscriptionPoll(answer: SubscriptionPollAnswer) {
        val teamId = currentTeamId ?: return
        val poll = _ui.value.subscriptionPoll ?: return
        if (_ui.value.pollVoting) return
        viewModelScope.launch {
            _ui.update { it.copy(pollVoting = true, pollError = null) }
            when (val result = chargeRepository.voteSubscriptionPoll(teamId, poll.id, answer)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(subscriptionPoll = result.data, pollVoting = false)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(pollVoting = false, pollError = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(pollVoting = false, pollError = "Нет соединения с сервером")
                }
            }
        }
    }

    fun trackAdClick() {
        val ad = _ui.value.advertisement ?: return
        viewModelScope.launch { advertising.recordAdEvent(ad.id, "CLICK", sessionId, currentTeamId) }
    }

    private suspend fun load(teamId: Long) {
        _ui.update { it.copy(loading = true) }
        loadSubscriptionPoll(teamId)
        loadNearestEvent(teamId)
        loadAdvertisement(teamId)
        if (_ui.value.advertisement == null) loadNotifications() else _ui.update {
            it.copy(notifications = emptyList())
        }
        _ui.update { it.copy(loading = false) }
    }

    private suspend fun loadSubscriptionPoll(teamId: Long) {
        when (val result = chargeRepository.currentSubscriptionPoll(teamId)) {
            is ApiResult.Success -> _ui.update { it.copy(subscriptionPoll = result.data, pollError = null) }
            else -> Unit
        }
    }

    private suspend fun loadAdvertisement(teamId: Long) {
        when (val result = advertising.activeAd(teamId)) {
            is ApiResult.Success -> {
                val previous = _ui.value.advertisement?.id
                _ui.update { it.copy(advertisement = result.data) }
                val ad = result.data
                if (ad != null && previous != ad.id) {
                    advertising.recordAdEvent(ad.id, "VIEW", sessionId, teamId)
                }
            }
            else -> _ui.update { it.copy(advertisement = null) }
        }
    }

    private suspend fun loadNearestEvent(teamId: Long) {
        val todayDate = LocalDate.now()
        val from = todayDate.withDayOfMonth(1).minusMonths(1)
        val to = java.time.YearMonth.from(todayDate).plusMonths(1).atEndOfMonth()
        when (val r = eventRepository.events(teamId, from.toString(), to.toString())) {
            is ApiResult.Success -> {
                val today = todayDate.toString()
                val nearest = r.data.filter { it.dateIso >= today }
                    .sortedWith(compareBy({ it.dateIso }, { it.time })).firstOrNull()
                _ui.update { it.copy(nearestEvent = nearest) }
            }
            else -> _ui.update { it.copy(nearestEvent = null) }
        }
    }

    private suspend fun loadNotifications() {
        when (val r = notificationRepository.list()) {
            is ApiResult.Success -> _ui.update {
                it.copy(notifications = r.data.sortedByDescending { n -> n.sentAtIso }.take(3))
            }
            else -> _ui.update { it.copy(notifications = emptyList()) }
        }
    }
}
