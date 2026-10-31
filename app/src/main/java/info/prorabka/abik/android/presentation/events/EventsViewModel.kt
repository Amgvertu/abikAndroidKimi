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
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val noTeam: Boolean = false,
        val section: EventSection = EventSection.TRAINING,
        val month: YearMonth = YearMonth.now(),
        val selectedDate: LocalDate = LocalDate.now(),
        val events: List<EventListItem> = emptyList(),
        val myRole: MemberRole? = null,
        val error: String? = null,
    ) {
        val isManager: Boolean
            get() = myRole == MemberRole.OWNER ||
                myRole == MemberRole.ADMIN ||
                myRole == MemberRole.MODERATOR

        val sectionEvents: List<EventListItem>
            get() = events.filter { it.section == section }

        /** Дни месяца, в которые есть события выбранного раздела. */
        val daysWithEvents: Set<LocalDate>
            get() = sectionEvents.mapNotNull {
                runCatching { LocalDate.parse(it.dateIso) }.getOrNull()
            }.toSet()

        /**
         * Маркеры дней календаря: дата → ключи «время|раздел» событий этого дня.
         * Цвет маркера определяется парой (время + тип события) на уровне UI.
         */
        val dayMarkerKeys: Map<LocalDate, List<String>>
            get() = sectionEvents
                .mapNotNull { e ->
                    runCatching { LocalDate.parse(e.dateIso) }.getOrNull()?.let { it to e }
                }
                .groupBy({ it.first }, { "${it.second.time}|${it.second.section.name}" })
                .mapValues { (_, keys) -> keys.distinct().sorted() }

        val selectedDayEvents: List<EventListItem>
            get() = sectionEvents.filter { it.dateIso == selectedDate.toString() }
                .sortedBy { it.time }

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

    fun selectSection(section: EventSection) {
        _ui.update { it.copy(section = section) }
    }

    fun selectDate(date: LocalDate) {
        _ui.update { it.copy(selectedDate = date) }
        if (YearMonth.from(date) != _ui.value.month) {
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
