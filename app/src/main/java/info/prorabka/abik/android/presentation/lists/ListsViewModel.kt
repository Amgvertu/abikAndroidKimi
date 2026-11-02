package info.prorabka.abik.android.presentation.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.ChargeList
import info.prorabka.abik.android.domain.model.MemberStatus
import info.prorabka.abik.android.domain.model.TeamMember
import info.prorabka.abik.android.domain.repository.ChargeRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Именованные списки получателей начислений (CRUD, управляющие). */
@HiltViewModel
class ListsViewModel @Inject constructor(
    private val chargeRepository: ChargeRepository,
    private val teamRepository: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val noTeam: Boolean = false,
        val saving: Boolean = false,
        val lists: List<ChargeList> = emptyList(),
        val members: List<TeamMember> = emptyList(),
        val error: String? = null,
        val info: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun load() {
        val teamId = teamManager.selected.value?.id
        if (teamId == null) {
            _ui.update { it.copy(loading = false, noTeam = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, noTeam = false, error = null) }
            when (val m = teamRepository.members(teamId)) {
                is ApiResult.Success -> _ui.update {
                    it.copy(members = m.data.filter { mm -> mm.status == MemberStatus.ACTIVE })
                }
                else -> Unit
            }
            when (val l = chargeRepository.chargeLists(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(lists = l.data) }
                is ApiResult.HttpError -> _ui.update { it.copy(error = l.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
            _ui.update { it.copy(loading = false) }
        }
    }

    /** Создать или обновить список (listId == null — создание). */
    fun save(listId: Long?, name: String, memberIds: Set<Long>, onDone: () -> Unit) {
        val teamId = teamManager.selected.value?.id ?: return
        when {
            name.isBlank() -> _ui.update { it.copy(error = "Укажите название списка") }
            memberIds.isEmpty() -> _ui.update {
                it.copy(error = "Выберите хотя бы одного участника")
            }
            else -> viewModelScope.launch {
                _ui.update { it.copy(saving = true, error = null) }
                val result = if (listId == null) {
                    chargeRepository.createChargeList(teamId, name.trim(), memberIds)
                } else {
                    chargeRepository.updateChargeList(teamId, listId, name.trim(), memberIds)
                }
                when (result) {
                    is ApiResult.Success -> {
                        _ui.update {
                            it.copy(
                                saving = false,
                                info = if (listId == null) "Список создан" else "Список обновлён",
                            )
                        }
                        onDone()
                        load()
                    }
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

    fun delete(list: ChargeList) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            when (val r = chargeRepository.deleteChargeList(teamId, list.id)) {
                is ApiResult.Success -> {
                    _ui.update {
                        it.copy(
                            lists = it.lists.filter { l -> l.id != list.id },
                            info = "Список удалён",
                        )
                    }
                }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(error = "Нет соединения с сервером")
                }
            }
        }
    }

    fun clearMessages() = _ui.update { it.copy(error = null, info = null) }
}
