package info.prorabka.abik.android.presentation.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeTemplate
import info.prorabka.abik.android.domain.model.EventChargeDistribution
import info.prorabka.abik.android.domain.model.EventChargeMode
import info.prorabka.abik.android.domain.model.EventFormData
import info.prorabka.abik.android.domain.model.EventSection
import info.prorabka.abik.android.domain.model.EventType
import info.prorabka.abik.android.domain.repository.ChargeRepository
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
    private val chargeRepository: ChargeRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    /** -1 = создание, иначе редактирование. */
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: -1L

    /** Предвыбранная дата из календаря (параметр навигации, необязательный). */
    private val presetDate: String? = savedStateHandle.get<String>("date")
        ?.takeIf { it.isNotBlank() }

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
        // Тренировка
        val repeatWeekly: Boolean = false,
        val weekdays: Set<Int> = emptySet(),
        val untilDateIso: String? = null,
        val isGameTraining: Boolean = false,
        // Регулярка / Двухсторонка
        val requiredPlayers: String = "",
        val chargeMode: EventChargeMode = EventChargeMode.NONE,
        val chargeAmountRub: String = "",
        val chargeDistribution: EventChargeDistribution = EventChargeDistribution.EACH_ATTENDED,
        val chargeTemplateId: Long? = null,
        val templates: List<ChargeTemplate> = emptyList(),
        // Служебное
        val seriesId: Long? = null,
        val askScope: Boolean = false,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null,
    ) {
        /** Место необязательно только для повторяющейся тренировки (ТЗ). */
        val placeRequired: Boolean
            get() = !(section == EventSection.TRAINING && repeatWeekly)

        /** Варианты распределения суммы: у двухсторонки только «отметившиеся». */
        val availableDistributions: List<EventChargeDistribution>
            get() = when (section) {
                EventSection.SCRIMMAGE -> listOf(
                    EventChargeDistribution.EACH_ATTENDED,
                    EventChargeDistribution.SPLIT_ATTENDED,
                )
                else -> EventChargeDistribution.entries
            }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        presetDate?.let { date -> _ui.update { it.copy(dateIso = date) } }
        if (isEdit) loadEvent()
        loadTemplates()
    }

    private fun loadTemplates() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            // В форме события доступны только шаблоны из группы «Регулярка» (ТЗ).
            val regularGroupIds = when (val g = chargeRepository.groups(teamId)) {
                is ApiResult.Success -> g.data
                    .filter { it.name.equals(REGULAR_GROUP_NAME, ignoreCase = true) }
                    .map { it.id }
                    .toSet()
                else -> emptySet()
            }
            when (val r = chargeRepository.templates(teamId, onlyActive = true)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(
                        templates = r.data.filter { t -> t.groupId in regularGroupIds },
                    )
                }
                else -> Unit
            }
        }
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
                            isGameTraining = e.section == EventSection.TRAINING &&
                                e.type == EventType.GAME,
                            requiredPlayers = e.requiredPlayers?.toString().orEmpty(),
                            chargeMode = e.chargeMode,
                            chargeAmountRub = e.chargeAmountKopecks
                                ?.let { k -> (k / 100).toString() }.orEmpty(),
                            chargeDistribution = e.chargeDistribution
                                ?: EventChargeDistribution.EACH_ATTENDED,
                            chargeTemplateId = e.chargeTemplateId,
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
            EventSection.TRAINING ->
                if (it.isGameTraining && !it.repeatWeekly) EventType.GAME else EventType.TRAINING
            EventSection.REGULAR, EventSection.SCRIMMAGE ->
                if (it.type == EventType.TRAINING) EventType.GAME else it.type
        }
        // У двухсторонки только «отметившиеся»
        val newDistribution = when {
            section == EventSection.SCRIMMAGE &&
                it.chargeDistribution == EventChargeDistribution.EACH_ROSTER ->
                EventChargeDistribution.EACH_ATTENDED
            section == EventSection.SCRIMMAGE &&
                it.chargeDistribution == EventChargeDistribution.SPLIT_ROSTER ->
                EventChargeDistribution.SPLIT_ATTENDED
            else -> it.chargeDistribution
        }
        it.copy(section = section, type = newType, chargeDistribution = newDistribution)
    }

    fun setType(type: EventType) = _ui.update { it.copy(type = type) }
    fun setDate(date: LocalDate) = _ui.update { it.copy(dateIso = date.toString()) }
    fun setTime(v: String) = _ui.update { it.copy(time = v) }
    fun setPlace(v: String) = _ui.update { it.copy(place = v) }
    fun setNote(v: String) = _ui.update { it.copy(note = v) }
    fun setUntilDate(date: LocalDate) = _ui.update { it.copy(untilDateIso = date.toString()) }
    fun setRequiredPlayers(v: String) = _ui.update {
        it.copy(requiredPlayers = v.filter { c -> c.isDigit() }.take(3))
    }
    fun setChargeMode(mode: EventChargeMode) = _ui.update { it.copy(chargeMode = mode) }
    fun setChargeAmount(v: String) = _ui.update {
        it.copy(chargeAmountRub = v.filter { c -> c.isDigit() }.take(9))
    }
    fun setChargeDistribution(d: EventChargeDistribution) = _ui.update {
        it.copy(chargeDistribution = d)
    }
    fun setChargeTemplate(id: Long?) = _ui.update { it.copy(chargeTemplateId = id) }

    /** Повторяющаяся/разовая тренировка. Сброс «игровой» у повторяющейся (по ТЗ нет). */
    fun setRepeatWeekly(v: Boolean) = _ui.update {
        val newType = when {
            it.section != EventSection.TRAINING -> it.type
            v -> EventType.TRAINING
            it.isGameTraining -> EventType.GAME
            else -> EventType.TRAINING
        }
        it.copy(repeatWeekly = v, type = newType, isGameTraining = if (v) false else it.isGameTraining)
    }

    /** Чекбокс «Игровая» (разовая тренировка): серверный type = GAME. */
    fun setGameTraining(v: Boolean) = _ui.update {
        it.copy(
            isGameTraining = v,
            type = if (it.section == EventSection.TRAINING) {
                if (v) EventType.GAME else EventType.TRAINING
            } else it.type,
        )
    }

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

    /** У тренировки названия в форме нет (ТЗ) — генерируем автоматически. */
    private fun effectiveTitle(s: UiState): String = when (s.section) {
        EventSection.TRAINING ->
            if (s.isGameTraining && !s.repeatWeekly) "Игровая тренировка" else "Тренировка"
        else -> s.title.trim()
    }

    private fun validate(): Boolean {
        val s = _ui.value
        val timeOk = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(s.time)
        val date = runCatching { LocalDate.parse(s.dateIso) }.getOrNull()
        val until = s.untilDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val message = when {
            s.section != EventSection.TRAINING && s.title.isBlank() -> "Введите название"
            s.placeRequired && s.place.isBlank() -> "Введите место"
            date == null -> "Некорректная дата"
            !timeOk -> "Время в формате ЧЧ:ММ (например, 21:30)"
            s.repeatWeekly && s.section == EventSection.TRAINING && s.weekdays.isEmpty() ->
                "Выберите дни недели для повторения"
            s.repeatWeekly && s.section == EventSection.TRAINING && until == null ->
                "Выберите дату окончания повторения"
            s.repeatWeekly && s.section == EventSection.TRAINING &&
                until != null && until.isBefore(date) ->
                "Дата окончания серии раньше даты начала"
            s.chargeMode == EventChargeMode.MANUAL &&
                s.chargeAmountRub.isNotBlank() && s.chargeAmountRub.toLongOrNull() == null ->
                "Некорректная сумма"
            s.chargeMode == EventChargeMode.TEMPLATE && s.chargeTemplateId == null ->
                "Выберите шаблон начисления"
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
        val amountRub = s.chargeAmountRub.toLongOrNull()
        // Начисление шлём только если выбран способ; «вручную» без суммы = без начисления
        val effectiveMode = when (s.chargeMode) {
            EventChargeMode.MANUAL ->
                if (amountRub != null && amountRub > 0) EventChargeMode.MANUAL
                else EventChargeMode.NONE
            else -> s.chargeMode
        }
        val data = EventFormData(
            title = effectiveTitle(s),
            section = s.section,
            type = s.type,
            dateIso = s.dateIso,
            time = s.time,
            place = s.place.trim(),
            note = s.note.trim().ifBlank { null },
            weekdays = if (s.repeatWeekly && s.section == EventSection.TRAINING && !isEdit) {
                s.weekdays
            } else {
                null
            },
            untilDateIso = if (s.repeatWeekly && s.section == EventSection.TRAINING && !isEdit) {
                s.untilDateIso
            } else {
                null
            },
            requiredPlayers = s.requiredPlayers.toIntOrNull(),
            chargeMode = effectiveMode,
            chargeAmountKopecks = if (effectiveMode == EventChargeMode.MANUAL) {
                amountRub?.times(100)
            } else {
                null
            },
            chargeDistribution = if (effectiveMode == EventChargeMode.MANUAL) {
                s.chargeDistribution
            } else {
                null
            },
            chargeTemplateId = if (effectiveMode == EventChargeMode.TEMPLATE) {
                s.chargeTemplateId
            } else {
                null
            },
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
        const val REGULAR_GROUP_NAME = "Регулярка"
    }
}
