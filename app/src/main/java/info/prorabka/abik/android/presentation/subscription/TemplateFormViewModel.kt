package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeDistribution
import info.prorabka.abik.android.domain.model.ChargeGroup
import info.prorabka.abik.android.domain.model.ChargeSubgroup
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
    /** Систематическое ежемесячное начисление участникам по списку. */
    SUBSCRIPTION("Абонемент", "Абонемент"),

    /** Начисление за регулярку на сезон; применяется из формы события. */
    REGULAR("Регулярка", "Регулярка"),
}

/**
 * Создание/редактирование шаблона начисления.
 * «Абонемент»: дата начисления, участники, чекбокс «начислять вратарям».
 * «Регулярка»: сезон, сумма, заявленные, способ распределения;
 * название формируется как «Регулярка. Сезон 2026/2027».
 */
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
        val chargeGoalies: Boolean = true,
        val groups: List<ChargeGroup> = emptyList(),
        val amountRubles: String = "",
        val dayOfMonth: String = "",
        val distribution: ChargeDistribution = ChargeDistribution.FIXED,
        val members: List<TeamMember> = emptyList(),
        val selectedUserIds: Set<Long> = emptySet(),
    ) {
        /** Итоговое название шаблона (для «Регулярки» — автоматическое). */
        val effectiveName: String
            get() = if (kind == TemplateKind.REGULAR) {
                "Регулярка. Сезон $season"
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
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> {
                    val active = m.data.filter { it.status == MemberStatus.ACTIVE }
                    _ui.update { it.copy(members = active) }
                }
                else -> Unit
            }
            if (templateId > 0) {
                when (val t = chargeRepository.templates(teamId)) {
                    is ApiResult.Success -> {
                        val template = t.data.firstOrNull { it.id == templateId }
                        if (template != null) {
                            val group = _ui.value.groups.firstOrNull { it.id == template.groupId }
                            val kind = if (
                                group?.name?.equals(
                                    TemplateKind.REGULAR.groupName,
                                    ignoreCase = true,
                                ) == true
                            ) {
                                TemplateKind.REGULAR
                            } else {
                                TemplateKind.SUBSCRIPTION
                            }
                            val season = if (kind == TemplateKind.REGULAR) {
                                Regex("""Сезон\s+(.+)$""")
                                    .find(template.name)?.groupValues?.get(1)?.trim().orEmpty()
                            } else {
                                ""
                            }
                            _ui.update {
                                it.copy(
                                    editTemplateId = template.id,
                                    kind = kind,
                                    name = template.name,
                                    season = season,
                                    amountRubles = (template.amountKopecks / 100).toString(),
                                    dayOfMonth = template.dayOfMonth?.toString() ?: "",
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
                name = if (kind == TemplateKind.SUBSCRIPTION && it.name.startsWith("Регулярка")) {
                    "Абонемент"
                } else {
                    it.name
                },
            )
        }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v.take(200)) }

    fun onSeason(v: String) {
        // Ввод только цифрами, разделитель подставляется автоматически: 20262027 → 2026/2027
        val digits = v.filter(Char::isDigit).take(8)
        val formatted = if (digits.length > 4) {
            digits.substring(0, 4) + "/" + digits.substring(4)
        } else {
            digits
        }
        _ui.update { it.copy(season = formatted) }
    }

    fun onChargeGoalies(checked: Boolean) {
        _ui.update {
            val goalieIds = it.members
                .filter { m -> Position.GOALKEEPER in m.positions }
                .map { m -> m.userId }
                .toSet()
            it.copy(
                chargeGoalies = checked,
                selectedUserIds = if (checked) {
                    it.selectedUserIds + goalieIds
                } else {
                    it.selectedUserIds - goalieIds
                },
            )
        }
    }

    fun onAmount(v: String) = _ui.update { it.copy(amountRubles = v.filter(Char::isDigit).take(9)) }

    fun onDayOfMonth(v: String) {
        val digits = v.filter(Char::isDigit).take(2)
        val n = digits.toIntOrNull()
        _ui.update {
            it.copy(dayOfMonth = if (n != null && n > 28) "28" else digits)
        }
    }

    fun onDistribution(v: ChargeDistribution) = _ui.update { it.copy(distribution = v) }

    fun toggleMember(userId: Long) = _ui.update {
        it.copy(
            selectedUserIds = if (userId in it.selectedUserIds) {
                it.selectedUserIds - userId
            } else {
                it.selectedUserIds + userId
            },
        )
    }

    fun selectAll() = _ui.update {
        it.copy(selectedUserIds = it.members.map { m -> m.userId }.toSet())
    }

    fun save(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val amount = s.amountRubles.toLongOrNull()
        val isRegular = s.kind == TemplateKind.REGULAR
        val day = if (isRegular) 1 else s.dayOfMonth.toIntOrNull()
        when {
            isRegular && s.season.isBlank() ->
                _ui.update { it.copy(error = "Укажите сезон, например 2026/2027") }
            !isRegular && s.name.isBlank() ->
                _ui.update { it.copy(error = "Укажите название шаблона") }
            amount == null || amount <= 0 ->
                _ui.update { it.copy(error = "Укажите сумму") }
            day == null || day !in 1..28 ->
                _ui.update { it.copy(error = "День месяца — от 1 до 28") }
            s.selectedUserIds.isEmpty() ->
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
                            _ui.update {
                                it.copy(saving = false, error = "Нет соединения с сервером")
                            }
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
                    dayOfMonth = day!!,
                    distribution = s.distribution,
                    memberIds = s.selectedUserIds,
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
