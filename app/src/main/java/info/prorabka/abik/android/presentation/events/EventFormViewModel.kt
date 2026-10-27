package info.prorabka.abik.android.presentation.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.EventFormData
import info.prorabka.abik.android.domain.model.EventSection
import info.prorabka.abik.android.domain.model.EventType
import info.prorabka.abik.android.domain.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EventFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    /** -1 = создание, иначе редактирование. */
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: -1L

    val isEdit: Boolean get() = eventId > 0

    data class UiState(
        val loading: Boolean = false,
        val title: String = "",
        val section: EventSection = EventSection.TRAINING,
        val type: EventType = EventType.TRAINING,
        val dateIso: String = LocalDate.now().toString(),
        val time: String = "21:00",
        val place: String = "",
        val note: String = "",
        val repeatWeekly: Boolean = false,
        val weekdays: Set<Int> = emptySet(),
        val untilDateIso: String? = null,
        val seriesId: Long? = null,
        val askScope: Boolean = false,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        if (isEdit) loadEvent()
    }

    private fun loadEvent() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true) }
            when (val r = eventRepository.detail(teamId, eventId)) {
                is ApiResult.Success -> {
                    val e = r.data
                    _ui.update {
                        it.copy(
                            loading = false,
                            title = e.title,
                            section = e.section,
                            type = e.type,
                            dateIso = e.dateIso,
                            time = e.time,
                            place = e.place,
                            note = e.note.orEmpty(),
                            seriesId = e.seriesId,
                        )
                    }
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(loading = false, error = r.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(loading = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun setTitle(v: String) = _ui.update { it.copy(title = v) }

    fun setSection(section: EventSection) = _ui.update {
        // Тип подгоняем под раздел
        val newType = when (section) {
            EventSection.TRAINING -> EventType.TRAINING
            EventSection.REGULAR ->
                if (it.type == EventType.TRAINING) EventType.GAME else it.type
        }
        it.copy(section = section, type = newType)
    }

    fun setType(type: EventType) = _ui.update { it.copy(type = type) }
    fun setDate(date: LocalDate) = _ui.update { it.copy(dateIso = date.toString()) }
    fun setTime(v: String) = _ui.update { it.copy(time = v) }
    fun setPlace(v: String) = _ui.update { it.copy(place = v) }
    fun setNote(v: String) = _ui.update { it.copy(note = v) }
    fun setRepeatWeekly(v: Boolean) = _ui.update { it.copy(repeatWeekly = v) }
    fun setUntilDate(date: LocalDate) = _ui.update { it.copy(untilDateIso = date.toString()) }

    fun toggleWeekday(day: Int) = _ui.update {
        val set = it.weekdays.toMutableSet()
        if (!set.remove(day)) set.add(day)
        it.copy(weekdays = set)
    }

    /** Точка входа сохранения: для события серии сначала спрашиваем scope. */
    fun save() {
        if (!validate()) return
        if (isEdit && _ui.value.seriesId != null) {
            _ui.update { it.copy(askScope = true) }
        } else {
            doSave("single")
        }
    }

    fun confirmScope(scope: String) {
        _ui.update { it.copy(askScope = false) }
        doSave(scope)
    }

    fun dismissScope() = _ui.update { it.copy(askScope = false) }

    private fun validate(): Boolean {
        val s = _ui.value
        val timeOk = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(s.time)
        val date = runCatching { LocalDate.parse(s.dateIso) }.getOrNull()
        val until = s.untilDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val message = when {
            s.title.isBlank() -> "Введите название"
            s.place.isBlank() -> "Введите место"
            date == null -> "Некорректная дата"
            !timeOk -> "Время в формате ЧЧ:ММ (например, 21:30)"
            s.repeatWeekly && s.weekdays.isEmpty() -> "Выберите дни недели для повторения"
            s.repeatWeekly && until == null -> "Выберите дату окончания повторения"
            s.repeatWeekly && until != null && until.isBefore(date) ->
                "Дата окончания серии раньше даты начала"
            else -> null
        }
        if (message != null) {
            _ui.update { it.copy(error = message) }
            return false
        }
        return true
    }

    private fun doSave(scope: String) {
        val teamId = teamManager.selected.value?.id ?: return
        val s = _ui.value
        val data = EventFormData(
            title = s.title.trim(),
            section = s.section,
            type = s.type,
            dateIso = s.dateIso,
            time = s.time,
            place = s.place.trim(),
            note = s.note.trim().ifBlank { null },
            weekdays = if (s.repeatWeekly && !isEdit) s.weekdays else null,
            untilDateIso = if (s.repeatWeekly && !isEdit) s.untilDateIso else null,
        )
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            val result = if (isEdit) {
                eventRepository.update(teamId, eventId, data, scope)
            } else {
                eventRepository.create(teamId, data)
            }
            when (result) {
                is ApiResult.Success -> _ui.update { it.copy(saving = false, saved = true) }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(saving = false, error = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }

    companion object {
        val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
