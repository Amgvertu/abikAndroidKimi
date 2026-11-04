package info.prorabka.abik.android.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.EventListItem
import info.prorabka.abik.android.domain.model.EventSection
import info.prorabka.abik.android.domain.model.MemberRole
import info.prorabka.abik.android.domain.repository.EventRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: info.prorabka.abik.android.domain.repository.UserRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val noTeam: Boolean = false,
        /** Включённые фильтры разделов; по умолчанию — все. */
        val sections: Set<EventSection> = EventSection.entries.toSet(),
        val month: YearMonth = YearMonth.now(),
        val selectedDate: LocalDate? = null,
        val events: List<EventListItem> = emptyList(),
        val myRole: MemberRole? = null,
        val myIsTrainer: Boolean = false,
        val error: String? = null,
    ) {
        val isManager: Boolean
            get() = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN || myIsTrainer

        val sectionEvents: List<EventListItem>
            get() = events.filter { it.section in sections }

        /** Самое частое время тренировок текущего месяца. При равенстве — самое раннее. */
        val dominantTrainingTimes: Set<String>
            get() {
                val counts = events.filter {
                    it.section == EventSection.TRAINING &&
                        runCatching { YearMonth.from(LocalDate.parse(it.dateIso)) == month }.getOrDefault(false)
                }.groupingBy { it.time }.eachCount()
                val maxCount = counts.values.maxOrNull() ?: return emptySet()
                return counts.filterValues { it == maxCount }.keys.minOrNull()?.let(::setOf).orEmpty()
            }

        val dayMarkerEvents: Map<LocalDate, List<EventListItem>>
            get() = sectionEvents.mapNotNull { event ->
                runCatching { LocalDate.parse(event.dateIso) }.getOrNull()?.let { it to event }
            }.groupBy({ it.first }, { it.second })

        val selectedDayEvents: List<EventListItem>
            get() = selectedDate?.let { date ->
                sectionEvents.filter { it.dateIso == date.toString() }.sortedBy { it.time }
            }.orEmpty()

        /** До трёх ближайших событий раздела, начиная с сегодня. */
        val upcoming: List<EventListItem>
            get() {
                val today = LocalDate.now().toString()
                return sectionEvents.filter { it.dateIso >= today }
                    .sortedWith(compareBy({ it.dateIso }, { it.time }))
                    .take(3)
            }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        load()
    }

    fun load() {
        val teamId = teamManager.selected.value?.id
        if (teamId == null) {
            _ui.update { it.copy(loading = false, refreshing = false, noTeam = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, noTeam = false, error = null) }
            when (val d = teamRepository.detail(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
                else -> Unit
            }
            val myId = (userRepository.me() as? ApiResult.Success)?.data?.id
            val members = (teamRepository.members(teamId) as? ApiResult.Success)?.data
            val isTrainer = members?.firstOrNull { it.userId == myId }?.isTrainer == true
            _ui.update { it.copy(myIsTrainer = isTrainer) }
            loadEvents(teamId)
        }
    }

    fun refresh() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(refreshing = true, error = null) }
            loadEvents(teamId)
        }
    }

    private suspend fun loadEvents(teamId: Long) {
        // Грузим диапазон: месяц ± 1, чтобы листать календарь без догрузки
        val from = _ui.value.month.minusMonths(1).atDay(1).toString()
        val to = _ui.value.month.plusMonths(1).atEndOfMonth().toString()
        when (val r = eventRepository.events(teamId, from, to)) {
            is ApiResult.Success -> _ui.update {
                it.copy(loading = false, refreshing = false, events = r.data)
            }
            is ApiResult.HttpError -> _ui.update {
                it.copy(loading = false, refreshing = false, error = r.message)
            }
            is ApiResult.NetworkError -> _ui.update {
                it.copy(loading = false, refreshing = false, error = "Нет соединения с сервером")
            }
        }
    }

    /** Фильтр раздела: тап включает/выключает раздел на календаре. */
    fun toggleSection(section: EventSection) {
        _ui.update {
            val next = if (section in it.sections) it.sections - section
            else it.sections + section
            it.copy(sections = next)
        }
    }

    fun selectDate(date: LocalDate) {
        val nextDate = if (_ui.value.selectedDate == date) null else date
        _ui.update { it.copy(selectedDate = nextDate) }
        if (nextDate != null && YearMonth.from(date) != _ui.value.month) {
            _ui.update { it.copy(month = YearMonth.from(date)) }
            reloadEvents()
        }
    }

    fun prevMonth() {
        _ui.update { it.copy(month = it.month.minusMonths(1)) }
        reloadEvents()
    }

    fun nextMonth() {
        _ui.update { it.copy(month = it.month.plusMonths(1)) }
        reloadEvents()
    }

    private fun reloadEvents() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch { loadEvents(teamId) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}
