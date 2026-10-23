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
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Создание/редактирование шаблона систематического начисления.
 * «Назначение» маппится на группу (+ опциональную подгруппу), как в разовом начислении.
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
        val name: String = "",
        val groups: List<ChargeGroup> = emptyList(),
        val subgroups: List<ChargeSubgroup> = emptyList(),
        val groupName: String = "",
        val subgroupName: String = "",
        val amountRubles: String = "",
        val dayOfMonth: String = "",
        val distribution: ChargeDistribution = ChargeDistribution.FIXED,
        val members: List<TeamMember> = emptyList(),
        val selectedUserIds: Set<Long> = emptySet(),
    )

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
                            var subgroupName = ""
                            if (group != null && template.subgroupId != null) {
                                when (val s = chargeRepository.subgroups(teamId, group.id)) {
                                    is ApiResult.Success -> {
                                        subgroupName = s.data
                                            .firstOrNull { it.id == template.subgroupId }
                                            ?.name ?: ""
                                        _ui.update { it.copy(subgroups = s.data) }
                                    }
                                    else -> Unit
                                }
                            }
                            _ui.update {
                                it.copy(
                                    editTemplateId = template.id,
                                    name = template.name,
                                    groupName = group?.name ?: "",
                                    subgroupName = subgroupName,
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

    fun onName(v: String) = _ui.update { it.copy(name = v.take(200)) }

    fun onGroupName(v: String) {
        _ui.update { it.copy(groupName = v, subgroupName = "", subgroups = emptyList()) }
        val teamId = teamManager.selected.value?.id ?: return
        val match = _ui.value.groups.firstOrNull { it.name.equals(v.trim(), ignoreCase = true) }
        if (match != null) {
            viewModelScope.launch {
                when (val s = chargeRepository.subgroups(teamId, match.id)) {
                    is ApiResult.Success -> _ui.update { it.copy(subgroups = s.data) }
                    else -> Unit
                }
            }
        }
    }

    fun onSubgroupName(v: String) = _ui.update { it.copy(subgroupName = v) }
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
        val day = s.dayOfMonth.toIntOrNull()
        when {
            s.name.isBlank() -> _ui.update { it.copy(error = "Укажите название шаблона") }
            s.groupName.isBlank() -> _ui.update { it.copy(error = "Укажите назначение") }
            amount == null || amount <= 0 -> _ui.update { it.copy(error = "Укажите сумму") }
            day == null || day !in 1..28 ->
                _ui.update { it.copy(error = "День месяца — от 1 до 28") }
            s.selectedUserIds.isEmpty() ->
                _ui.update { it.copy(error = "Выберите хотя бы одного участника") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }

                var group = s.groups.firstOrNull { it.name.equals(s.groupName.trim(), true) }
                if (group == null) {
                    when (val created = chargeRepository.createGroup(teamId, s.groupName.trim())) {
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

                var subgroupId: Long? = null
                if (s.subgroupName.isNotBlank()) {
                    val existing = s.subgroups.firstOrNull {
                        it.name.equals(s.subgroupName.trim(), true)
                    }
                    subgroupId = if (existing != null) {
                        existing.id
                    } else {
                        when (
                            val created = chargeRepository.createSubgroup(
                                teamId, group!!.id, s.subgroupName.trim(),
                            )
                        ) {
                            is ApiResult.Success -> created.data.id
                            else -> null
                        }
                    }
                }

                val result = chargeRepository.saveTemplate(
                    teamId = teamId,
                    templateId = s.editTemplateId,
                    groupId = group!!.id,
                    subgroupId = subgroupId,
                    name = s.name.trim(),
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
