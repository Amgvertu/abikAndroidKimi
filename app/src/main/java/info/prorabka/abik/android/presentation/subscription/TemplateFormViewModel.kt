package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeDistribution
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeList
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.Position
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Вид шаблона по ТЗ «Исправление функционала». */
enum class TemplateKind(val display: String, val groupName: String) {
    SUBSCRIPTION("Абонемент", "Абонемент"),
    POLL_SUBSCRIPTION("Абонемент (опрос)", "Абонемент"),
    REGULAR("Регулярка", "Регулярка"),
}

private fun formatSeasonDigits(digits: String): String =
    if (digits.length > 4) digits.take(4) + "/" + digits.drop(4) else digits

/** Создание и редактирование шаблона начисления. */
@HiltViewModel
class TemplateFormViewModel @Inject constructor(
    private val chargeRepository: ChargeRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null,
        val editTemplateId: Long? = null,
        val kind: TemplateKind = TemplateKind.SUBSCRIPTION,
        val name: String = "Абонемент",
        val season: String = "",
        val groups: List<ChargeGroup> = emptyList(),
        val amountRubles: String = "",
        val dayOfMonth: String = "",
        val pollStartDay: String = "",
        val pollEndDay: String = "",
        val distribution: ChargeDistribution = ChargeDistribution.FIXED,
        val members: List<TeamMember> = emptyList(),
        val selectedUserIds: Set<Long> = emptySet(),
        val lists: List<ChargeList> = emptyList(),
    ) {
        val effectiveName: String
            get() = if (kind == TemplateKind.REGULAR) {
                "Регулярка. Сезон ${formatSeasonDigits(season)}"
            } else {
                name
            }
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(templateId: Long) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val g = chargeRepository.groups(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(groups = g.data) }
                else -> Unit
            }
            when (val l = chargeRepository.chargeLists(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(lists = l.data) }
                else -> Unit
            }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> {
                    val chargeable = m.data.filter {
                        it.status == MemberStatus.ACTIVE &&
                            !it.isTrainer &&
                            Position.GOALKEEPER !in it.positions
                    }
                    _ui.update { it.copy(members = chargeable) }
                }
                else -> Unit
            }
            if (templateId > 0) {
                when (val t = chargeRepository.templates(teamId)) {
                    is ApiResult.Success -> {
                        val template = t.data.firstOrNull { it.id == templateId }
                        if (template != null) {
                            val group = _ui.value.groups.firstOrNull { it.id == template.groupId }
                            val kind = when {
                                template.pollEnabled -> TemplateKind.POLL_SUBSCRIPTION
                                group?.name?.equals(TemplateKind.REGULAR.groupName, true) == true ->
                                    TemplateKind.REGULAR
                                else -> TemplateKind.SUBSCRIPTION
                            }
                            val season = if (kind == TemplateKind.REGULAR) {
                                Regex("""Сезон\s+(.+)$""")
                                    .find(template.name)?.groupValues?.get(1)
                                    ?.filter(Char::isDigit).orEmpty()
                            } else ""
                            _ui.update {
                                it.copy(
                                    editTemplateId = template.id,
                                    kind = kind,
                                    name = template.name,
                                    season = season,
                                    amountRubles = (template.amountKopecks / 100).toString(),
                                    dayOfMonth = template.dayOfMonth?.toString().orEmpty(),
                                    pollStartDay = template.pollStartDay?.toString().orEmpty(),
                                    pollEndDay = template.pollEndDay?.toString().orEmpty(),
                                    distribution = template.distribution,
                                    selectedUserIds = template.memberIds,
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    fun onKind(kind: TemplateKind) {
        if (_ui.value.editTemplateId != null) return
        _ui.update {
            it.copy(
                kind = kind,
                name = if (kind != TemplateKind.REGULAR && it.name.startsWith("Регулярка")) {
                    "Абонемент"
                } else it.name,
            )
        }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v.take(200)) }
    fun onSeason(v: String) = _ui.update { it.copy(season = v.filter(Char::isDigit).take(8)) }
    fun onAmount(v: String) = _ui.update { it.copy(amountRubles = v.filter(Char::isDigit).take(9)) }

    fun onDayOfMonth(v: String) = _ui.update { it.copy(dayOfMonth = limitedDay(v, 28)) }
    fun onPollStartDay(v: String) = _ui.update { it.copy(pollStartDay = limitedDay(v, 31)) }
    fun onPollEndDay(v: String) = _ui.update { it.copy(pollEndDay = limitedDay(v, 31)) }
    private fun limitedDay(value: String, max: Int): String {
        val digits = value.filter(Char::isDigit).take(2)
        val number = digits.toIntOrNull()
        return if (number != null && number > max) max.toString() else digits
    }

    fun onDistribution(v: ChargeDistribution) = _ui.update { it.copy(distribution = v) }
    fun toggleMember(userId: Long) = _ui.update {
        it.copy(selectedUserIds = if (userId in it.selectedUserIds) {
            it.selectedUserIds - userId
        } else it.selectedUserIds + userId)
    }
    fun selectAll() = _ui.update { it.copy(selectedUserIds = it.members.map { m -> m.userId }.toSet()) }
    fun applyList(list: ChargeList) = _ui.update {
        val memberIds = it.members.map { m -> m.userId }.toSet()
        it.copy(selectedUserIds = list.memberIds.intersect(memberIds))
    }

    fun save(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val amount = s.amountRubles.toLongOrNull()
        val isRegular = s.kind == TemplateKind.REGULAR
        val isPoll = s.kind == TemplateKind.POLL_SUBSCRIPTION
        val day = when {
            isRegular -> 1
            isPoll -> null
            else -> s.dayOfMonth.toIntOrNull()
        }
        val pollStart = s.pollStartDay.toIntOrNull()
        val pollEnd = s.pollEndDay.toIntOrNull()
        when {
            isRegular && s.season.isBlank() ->
                _ui.update { it.copy(error = "Укажите сезон, например 2026/2027") }
            !isRegular && s.name.isBlank() ->
                _ui.update { it.copy(error = "Укажите название шаблона") }
            amount == null || amount <= 0 ->
                _ui.update { it.copy(error = "Укажите сумму") }
            isPoll && (pollStart == null || pollStart !in 1..31 || pollEnd == null || pollEnd !in 1..31) ->
                _ui.update { it.copy(error = "Дни опроса — от 1 до 31") }
            isPoll && pollEnd!! < pollStart!! ->
                _ui.update { it.copy(error = "День окончания опроса не может быть меньше дня начала") }
            !isPoll && (day == null || day !in 1..28) ->
                _ui.update { it.copy(error = "День месяца — от 1 до 28") }
            !isPoll && s.selectedUserIds.isEmpty() ->
                _ui.update { it.copy(error = "Выберите хотя бы одного участника") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }
                val groupName = s.kind.groupName
                var group = s.groups.firstOrNull { it.name.equals(groupName, true) }
                if (group == null) {
                    when (val created = chargeRepository.createGroup(teamId, groupName)) {
                        is ApiResult.Success -> group = created.data
                        is ApiResult.HttpError -> {
                            _ui.update { it.copy(saving = false, error = created.message) }
                            return@launch
                        }
                        is ApiResult.NetworkError -> {
                            _ui.update { it.copy(saving = false, error = "Нет соединения с сервером") }
                            return@launch
                        }
                    }
                }
                val result = chargeRepository.saveTemplate(
                    teamId = teamId,
                    templateId = s.editTemplateId,
                    groupId = group!!.id,
                    subgroupId = null,
                    name = s.effectiveName.trim(),
                    amountKopecks = amount!! * 100,
                    dayOfMonth = day,
                    distribution = s.distribution,
                    memberIds = if (isPoll) emptySet() else s.selectedUserIds,
                    pollStartDay = if (isPoll) pollStart else null,
                    pollEndDay = if (isPoll) pollEnd else null,
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
