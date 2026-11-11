package info.prorabka.abik.android.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import info.prorabka.abik.android.core.CurrentTeamManager
import info.prorabka.abik.android.core.network.ApiResult
import info.prorabka.abik.android.domain.model.*
import info.prorabka.abik.android.domain.repository.ExpenseRepository
import info.prorabka.abik.android.domain.repository.ReportRepository
import info.prorabka.abik.android.domain.repository.TeamRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val reports: ReportRepository,
    private val teams: TeamRepository,
    private val teamManager: CurrentTeamManager,
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val saving: Boolean = false,
        val exporting: Boolean = false,
        val exportedFile: File? = null,
        val expenses: List<TeamExpense> = emptyList(),
        val selectedMonth: YearMonth = YearMonth.now(),
        val periodFrom: LocalDate? = null,
        val periodTo: LocalDate? = null,
        val myRole: MemberRole? = null,
        val error: String? = null,
    ) {
        val canEdit: Boolean get() = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN
        val isOwner: Boolean get() = myRole == MemberRole.OWNER
        val filteredExpenses: List<TeamExpense>
            get() = expenses.filter { expense ->
                val date = runCatching { LocalDate.parse(expense.expenseDate) }.getOrNull()
                    ?: return@filter false
                if (periodFrom != null && periodTo != null) {
                    !date.isBefore(periodFrom) && !date.isAfter(periodTo)
                } else {
                    YearMonth.from(date) == selectedMonth
                }
            }
        val total: Long get() = filteredExpenses.sumOf { it.amountKopecks }
    }
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()
    init { load() }
    fun previousMonth() = _ui.update {
        it.copy(selectedMonth = it.selectedMonth.minusMonths(1), periodFrom = null, periodTo = null)
    }
    fun nextMonth() = _ui.update {
        it.copy(selectedMonth = it.selectedMonth.plusMonths(1), periodFrom = null, periodTo = null)
    }
    fun setPeriod(from: LocalDate, to: LocalDate) = _ui.update {
        it.copy(periodFrom = from, periodTo = to)
    }
    fun resetPeriod() = _ui.update { it.copy(periodFrom = null, periodTo = null) }
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
    fun export() {
        val teamId = teamManager.selected.value?.id ?: return
        val state = _ui.value
        if (!state.isOwner || state.exporting) return
        val from = state.periodFrom ?: state.selectedMonth.atDay(1)
        val to = state.periodTo ?: state.selectedMonth.atEndOfMonth()
        viewModelScope.launch {
            _ui.update { it.copy(exporting = true, error = null) }
            when (val result = reports.exportExpenses(teamId, from.toString(), to.toString())) {
                is ApiResult.Success -> _ui.update {
                    it.copy(exporting = false, exportedFile = result.data)
                }
                is ApiResult.HttpError -> _ui.update {
                    it.copy(exporting = false, error = result.message)
                }
                is ApiResult.NetworkError -> _ui.update {
                    it.copy(exporting = false, error = "Нет соединения с сервером")
                }
            }
        }
    }
    fun consumeExportedFile() = _ui.update { it.copy(exportedFile = null) }
    fun save(id: Long?, data: ExpenseFormData, onDone: () -> Unit) {
        val teamId = teamManager.selected.value?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            val result = if (id == null) repository.create(teamId, data) else repository.update(teamId, id, data)
            when (result) {
                is ApiResult.Success -> {
                    val expenseMonth = runCatching {
                        YearMonth.from(LocalDate.parse(data.expenseDate))
                    }.getOrNull()
                    _ui.update {
                        it.copy(
                            saving = false,
                            selectedMonth = expenseMonth ?: it.selectedMonth,
                            periodFrom = null,
                            periodTo = null,
                        )
                    }
                    onDone()
                    load()
                }
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
