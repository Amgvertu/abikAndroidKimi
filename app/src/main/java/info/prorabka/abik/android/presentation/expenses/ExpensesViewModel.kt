package info.prorabka.abik.android.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.*
import info.prorabka.abik.android.domain.repository.ExpenseRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val teams: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val expenses: List<TeamExpense> = emptyList(),
        val myRole: MemberRole? = null,
        val error: String? = null,
    ) {
        val canEdit: Boolean get() = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN
        val total: Long get() = expenses.sumOf { it.amountKopecks }
    }
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    init { load() }
    fun load() {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val d = teams.detail(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(myRole = d.data.myRole) }
                else -> Unit
            }
            when (val r = repository.list(teamId)) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, expenses = r.data) }
                is ApiResult.HttpError -> _ui.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _ui.update { it.copy(loading = false, error = "Нет соединения с сервером") }
            }
        }
    }
    fun save(id: Long?, data: ExpenseFormData, onDone: () -> Unit) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            val result = if (id == null) repository.create(teamId, data) else repository.update(teamId, id, data)
            when (result) {
                is ApiResult.Success -> { _ui.update { it.copy(saving = false) }; onDone(); load() }
                is ApiResult.HttpError -> _ui.update { it.copy(saving = false, error = result.message) }
                is ApiResult.NetworkError -> _ui.update { it.copy(saving = false, error = "Нет соединения с сервером") }
            }
        }
    }
    fun delete(id: Long, onDone: () -> Unit) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            when (val r = repository.delete(teamId, id)) {
                is ApiResult.Success -> { onDone(); load() }
                is ApiResult.HttpError -> _ui.update { it.copy(error = r.message) }
                is ApiResult.NetworkError -> _ui.update { it.copy(error = "Нет соединения с сервером") }
            }
        }
    }
}
