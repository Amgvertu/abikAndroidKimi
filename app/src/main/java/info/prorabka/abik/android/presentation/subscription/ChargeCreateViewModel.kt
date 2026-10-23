package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Создание разового начисления. Форма по ТЗ v3: назначение, сумма, дата, примечание.
 * «Назначение» маппится на группу (+ опциональная подгруппа) серверной модели
 * с автодополнением; новая группа/подгруппа создаётся при сохранении.
 */
@HiltViewModel
class ChargeCreateViewModel @Inject constructor(
    private val chargeRepository: ChargeRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val error: String? = null,
        val groups: List<ChargeGroup> = emptyList(),
        val subgroups: List<ChargeSubgroup> = emptyList(),
        val groupName: String = "",
        val subgroupName: String = "",
        val amountRubles: String = "",
        val dueDate: LocalDate = LocalDate.now(),
        val note: String = "",
        val members: List<TeamMember> = emptyList(),
        val selectedUserIds: Set<Long> = emptySet(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load(preselectUserId: Long) {
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
                    _ui.update {
                        it.copy(
                            members = active,
                            selectedUserIds = if (preselectUserId > 0) {
                                setOf(preselectUserId)
                            } else {
                                emptySet()
                            },
                        )
                    }
                }
                else -> Unit
            }
            _ui.update { it.copy(loading = false) }
        }
    }

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
    fun onDueDate(v: LocalDate) = _ui.update { it.copy(dueDate = v) }
    fun onNote(v: String) = _ui.update { it.copy(note = v.take(500)) }

    fun toggleMember(userId: Long) = _ui.update {
        it.copy(
            selectedUserIds = if (userId in it.selectedUserIds) {
                it.selectedUserIds - userId
            } else {
                it.selectedUserIds + userId
            },
        )
    }

    fun selectAll() = _ui.update { it.copy(selectedUserIds = it.members.map { m -> m.userId }.toSet()) }

    fun save(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val amount = s.amountRubles.toLongOrNull()
        when {
            s.groupName.isBlank() -> _ui.update { it.copy(error = "Укажите назначение") }
            amount == null || amount <= 0 -> _ui.update { it.copy(error = "Укажите сумму") }
            s.selectedUserIds.isEmpty() ->
                _ui.update { it.copy(error = "Выберите хотя бы одного участника") }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }

                // Группа: найти по имени или создать
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

                // Подгруппа (опционально): найти или создать
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

                val result = chargeRepository.createManual(
                    teamId = teamId,
                    groupId = group!!.id,
                    subgroupId = subgroupId,
                    userIds = s.selectedUserIds,
                    amountKopecks = amount!! * 100,
                    description = s.note.ifBlank { null },
                    dueDateIso = s.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
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
