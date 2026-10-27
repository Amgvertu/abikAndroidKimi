package info.prorabka.abik.android.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.core.util.filterMoneyInput
import info.prorabka.abik.android.core.util.kopecksToInput
import info.prorabka.abik.android.core.util.parseMoneyToKopecks
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
 * Создание и редактирование разового начисления (ТЗ 7.1).
 * Форма по ТЗ v3: назначение, сумма, дата, примечание.
 * «Назначение» маппится на группу (+ опциональная подгруппа) серверной модели
 * с автодополнением; новая группа/подгруппа создаётся при сохранении.
 *
 * Режим редактирования (chargeId > 0): участник не меняется (блок участников скрыт),
 * доступно удаление. Сервер запрещает уменьшать сумму ниже уже оплаченной.
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
        val editChargeId: Long? = null,
        /** Уже оплаченная сумма (в режиме редактирования) — ниже неё уменьшать нельзя. */
        val paidKopecks: Long = 0,
        /** Имя участника начисления (в режиме редактирования, для подсказки). */
        val chargeUserName: String = "",
    ) {
        val isEdit: Boolean get() = editChargeId != null
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    /** chargeId > 0 — режим редактирования; preselectUserId > 0 — предвыбор участника. */
    fun load(preselectUserId: Long, chargeId: Long = -1L) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    loading = true,
                    error = null,
                    editChargeId = chargeId.takeIf { id -> id > 0 },
                )
            }
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
            if (chargeId > 0) {
                loadChargeForEdit(teamId, chargeId)
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    /** Предзаполнение формы данными начисления (режим редактирования). */
    private suspend fun loadChargeForEdit(teamId: Long, chargeId: Long) {
        when (val c = chargeRepository.chargeById(teamId, chargeId)) {
            is ApiResult.Success -> {
                val charge = c.data
                _ui.update {
                    it.copy(
                        groupName = charge.groupName,
                        subgroupName = charge.subgroupName.orEmpty(),
                        amountRubles = charge.amountKopecks.kopecksToInput(),
                        dueDate = charge.dueDate
                            ?.let { d -> runCatching { LocalDate.parse(d) }.getOrNull() }
                            ?: LocalDate.now(),
                        note = charge.description.orEmpty(),
                        selectedUserIds = setOf(charge.userId),
                        paidKopecks = charge.paidKopecks,
                        chargeUserName = charge.userName,
                    )
                }
                // Подгружаем подгруппы выбранной группы, чтобы автодополнение работало
                val group = _ui.value.groups.firstOrNull {
                    it.name.equals(charge.groupName, ignoreCase = true)
                }
                if (group != null) {
                    when (val s = chargeRepository.subgroups(teamId, group.id)) {
                        is ApiResult.Success -> _ui.update { it.copy(subgroups = s.data) }
                        else -> Unit
                    }
                }
            }
            is ApiResult.HttpError -> _ui.update { it.copy(error = c.message) }
            is ApiResult.NetworkError -> _ui.update {
                it.copy(error = "Нет соединения с сервером")
            }
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
    fun onAmount(v: String) = _ui.update { it.copy(amountRubles = v.filterMoneyInput()) }
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

    /** Переключатель «все участники»: если выбраны все — снимает выбор, иначе выбирает всех. */
    fun toggleSelectAll() = _ui.update {
        val all = it.members.map { m -> m.userId }.toSet()
        it.copy(
            selectedUserIds = if (it.selectedUserIds.containsAll(all)) emptySet() else all,
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _ui.value
        val teamId = teamManager.selected.value?.id ?: return
        val amountKopecks = s.amountRubles.parseMoneyToKopecks()
        when {
            s.groupName.isBlank() -> _ui.update { it.copy(error = "Укажите назначение") }
            amountKopecks == null || amountKopecks <= 0 ->
                _ui.update { it.copy(error = "Укажите сумму") }
            !s.isEdit && s.selectedUserIds.isEmpty() ->
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

                val result = if (s.editChargeId != null) {
                    chargeRepository.updateCharge(
                        teamId = teamId,
                        chargeId = s.editChargeId,
                        groupId = group!!.id,
                        subgroupId = subgroupId,
                        amountKopecks = amountKopecks!!,
                        description = s.note.ifBlank { null },
                        dueDateIso = s.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    )
                } else {
                    chargeRepository.createManual(
                        teamId = teamId,
                        groupId = group!!.id,
                        subgroupId = subgroupId,
                        userIds = s.selectedUserIds,
                        amountKopecks = amountKopecks!!,
                        description = s.note.ifBlank { null },
                        dueDateIso = s.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    )
                }
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

    /** Удаление начисления (только режим редактирования). Сервер запретит, если были оплаты. */
    fun delete(onDone: () -> Unit) {
        val chargeId = _ui.value.editChargeId ?: return
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            when (val r = chargeRepository.deleteCharge(teamId, chargeId)) {
                is ApiResult.Success -> onDone()
                is ApiResult.HttpError -> _ui.update { it.copy(saving = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(saving = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
}
